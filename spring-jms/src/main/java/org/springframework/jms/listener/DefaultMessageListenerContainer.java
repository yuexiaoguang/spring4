package org.springframework.jms.listener;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Session;

import org.springframework.core.Constants;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jms.JmsException;
import org.springframework.jms.support.JmsUtils;
import org.springframework.jms.support.destination.CachingDestinationResolver;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.scheduling.SchedulingAwareRunnable;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.BackOffExecution;
import org.springframework.util.backoff.FixedBackOff;

/**
 * 使用普通JMS客户端API的消息监听器容器变体, 特别是{@code MessageConsumer.receive()}调用的循环,
 * 它还允许事务性地接收消息 (使用XA事务注册它们).
 * 设计用于在本机JMS环境以及Java EE环境中工作, 只有很小的配置差异.
 *
 * <p>这是一个简单但功能强大的消息监听器容器形式.
 * 在启动时, 它获取固定数量的JMS会话以调用监听器, 并可选地允许在运行时动态调整 (最大数量).
 * 与{@link SimpleMessageListenerContainer}一样, 它的主要优点是运行时复杂度低, 特别是对JMS提供者的最低要求:
 * 甚至不需要JMS {@code ServerSessionPool}工具.
 * 除此之外, 它是完全自我恢复的, 以防代理暂时不可用, 并允许停止/重新启动以及运行时更改其配置.
 *
 * <p>实际的{@code MessageListener}执行发生在
 * 通过Spring的{@link org.springframework.core.task.TaskExecutor TaskExecutor}抽象创建的异步工作单元中.
 * 默认情况下, 根据{@link #setConcurrentConsumers "concurrentConsumers"}设置, 将在启动时创建指定数量的调用者任务.
 * 指定备用{@code TaskExecutor}以与现有线程池工具 (例如Java EE服务器)集成,
 * 例如使用{@link org.springframework.scheduling.commonj.WorkManagerTaskExecutor CommonJ WorkManager}.
 * 使用本机JMS设置, 每个监听器线程将使用缓存的JMS {@code Session} 和 {@code MessageConsumer} (仅在发生故障时刷新),
 * 尽可能高效地使用JMS提供者的资源.
 *
 * <p>通过将Spring {@link org.springframework.transaction.PlatformTransactionManager}
 * 传递到{@link #setTransactionManager "transactionManager"}属性, 可以自动将消息接收和监听器执行包装在事务中.
 * 这通常是Java EE环境中的{@link org.springframework.transaction.jta.JtaTransactionManager},
 * 以及从JNDI获取的JTA感知JMS {@code ConnectionFactory} (请查看Java EE服务器的文档).
 * 请注意, 如果指定了外部事务管理器, 则此监听器容器将自动为每个事务重新获取所有JMS句柄,
 * 以便与所有Java EE服务器(特别是JBoss)兼容.
 * 可以通过{@link #setCacheLevel "cacheLevel"} / {@link #setCacheLevelName "cacheLevelName"}属性覆盖此非缓存行为,
 * 强制缓存{@code Connection} (或{@code Session} 和 {@code MessageConsumer}), 即使涉及外部事务管理器.
 *
 * <p>通过指定高于{@link #setConcurrentConsumers "concurrentConsumers"}值
 * 的{@link #setMaxConcurrentConsumers "maxConcurrentConsumers"}值, 可以激活并发调用器数量的动态扩展.
 * 由于后者的默认值为1, 因此也可以简单地指定"maxConcurrentConsumers" e.g. 5,
 * 这将导致在消息负载增加的情况下动态扩展到5个并发消费者, 并且一旦负载减少, 动态缩减回标准消费者数量.
 * 考虑调整{@link #setIdleTaskExecutionLimit "idleTaskExecutionLimit"}设置来控制每个新任务的生命周期,
 * 以避免频繁放大和缩小, 特别是如果{@code ConnectionFactory}没有使用池JMS {@code Sessions}
 * 和/或{@code TaskExecutor}不是池线程 (检查配置!).
 * 请注意, 动态缩放只对第一个队列真正有意义;
 * 对于某个topic, 通常会使用默认的1个消费者数, 否则将在同一节点上多次收到相同的消息.
 *
 * <p><b>Note: 不要将Spring的{@link org.springframework.jms.connection.CachingConnectionFactory}与动态缩放结合使用.</b>
 * 理想情况下, 根本不要将它与消息监听器容器一起使用, 因为通常最好让监听器容器本身在其生命周期内处理适当的缓存.
 * 此外, 停止和重新启动监听器容器只能使用独立的本地缓存Connection - 而不是外部缓存连接.
 *
 * <p><b>强烈建议将{@link #setSessionTransacted "sessionTransacted"}设置为"true",
 * 或指定外部{@link #setTransactionManager "transactionManager"}.</b>
 * 有关确认模式和本机事务选项的详细信息, 请参阅{@link AbstractMessageListenerContainer} javadoc,
 * 有关配置外部事务管理器的详细信息, 请参阅{@link AbstractPollingMessageListenerContainer} javadoc.
 * 请注意, 对于默认的"AUTO_ACKNOWLEDGE"模式, 此容器在监听器执行之前应用自动消息确认, 并且在异常情况下不会重新传递.
 */
public class DefaultMessageListenerContainer extends AbstractPollingMessageListenerContainer {

	/**
	 * 默认线程名称前缀: "DefaultMessageListenerContainer-".
	 */
	public static final String DEFAULT_THREAD_NAME_PREFIX =
			ClassUtils.getShortName(DefaultMessageListenerContainer.class) + "-";

	/**
	 * 默认恢复间隔: 5000 ms = 5 seconds.
	 */
	public static final long DEFAULT_RECOVERY_INTERVAL = 5000;


	/**
	 * 根本不缓存JMS资源.
	 */
	public static final int CACHE_NONE = 0;

	/**
	 * 为每个监听器线程缓存共享JMS {@code Connection}.
	 */
	public static final int CACHE_CONNECTION = 1;

	/**
	 * 为每个监听器线程缓存共享JMS {@code Connection}和JMS {@code Session}.
	 */
	public static final int CACHE_SESSION = 2;

	/**
	 * 为每个监听器线程缓存共享JMS {@code Connection}, JMS {@code Session}, 和JMS MessageConsumer.
	 */
	public static final int CACHE_CONSUMER = 3;

	/**
	 * 自动选择适当缓存级别 (取决于事务管理策略).
	 */
	public static final int CACHE_AUTO = 4;


	private static final Constants constants = new Constants(DefaultMessageListenerContainer.class);


	private Executor taskExecutor;

	private BackOff backOff = new FixedBackOff(DEFAULT_RECOVERY_INTERVAL, Long.MAX_VALUE);

	private int cacheLevel = CACHE_AUTO;

	private int concurrentConsumers = 1;

	private int maxConcurrentConsumers = 1;

	private int maxMessagesPerTask = Integer.MIN_VALUE;

	private int idleConsumerLimit = 1;

	private int idleTaskExecutionLimit = 1;

	private final Set<AsyncMessageListenerInvoker> scheduledInvokers = new HashSet<AsyncMessageListenerInvoker>();

	private int activeInvokerCount = 0;

	private int registeredWithDestination = 0;

	private volatile boolean recovering = false;

	private volatile boolean interrupted = false;

	private Runnable stopCallback;

	private Object currentRecoveryMarker = new Object();

	private final Object recoveryMonitor = new Object();


	/**
	 * 设置用于运行监听器线程的Spring {@code TaskExecutor}.
	 * <p>默认是{@link org.springframework.core.task.SimpleAsyncTaskExecutor},
	 * 根据指定的并发消费者数量启动一些新线程.
	 * <p>指定备用{@code TaskExecutor}以与现有线程池集成.
	 * 请注意, 如果以特定方式管理线程, 例如在Java EE环境中, 这实际上只会增加值.
	 * 普通线程池不会添加太多值, 因为此监听器容器将在其整个生命周期中占用许多线程.
	 */
	public void setTaskExecutor(Executor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * 指定用于计算恢复尝试之间间隔的{@link BackOff}实例.
	 * 如果{@link BackOffExecution}实现返回 {@link BackOffExecution#STOP}, 则此监听器容器将不再进一步尝试恢复.
	 * <p>设置此属性时, 将忽略{@link #setRecoveryInterval(long) 恢复间隔}.
	 */
	public void setBackOff(BackOff backOff) {
		this.backOff = backOff;
	}

	/**
	 * 指定恢复尝试之间的间隔, 以<b>毫秒</b>为单位.
	 * 默认值为5000 ms, 即5秒. 这是一种使用指定间隔创建{@link FixedBackOff}的便捷方法.
	 * <p>有关更多恢复选项, 请考虑指定{@link BackOff}实例.
	 */
	public void setRecoveryInterval(long recoveryInterval) {
		this.backOff = new FixedBackOff(recoveryInterval, Long.MAX_VALUE);
	}

	/**
	 * 以相应常量的名称的形式, 指定允许此监听器容器应用的缓存级别: e.g. "CACHE_CONNECTION".
	 */
	public void setCacheLevelName(String constantName) throws IllegalArgumentException {
		if (constantName == null || !constantName.startsWith("CACHE_")) {
			throw new IllegalArgumentException("Only cache constants allowed");
		}
		setCacheLevel(constants.asNumber(constantName).intValue());
	}

	/**
	 * 指定允许此监听器容器应用的缓存级别.
	 * <p>如果已指定外部事务管理器(在外部事务范围内重新获得所有资源), 则默认为{@link #CACHE_NONE},
	 * 否则为{@link #CACHE_CONSUMER} (使用本地JMS资源操作).
	 * <p>在新获得的JMS {@code Connection} 和 {@code Session}的情况下,
	 * 某些Java EE服务器仅使用正在进行的XA事务注册其JMS资源, 这就是为什么默认情况下此监听器容器不会缓存这些.
	 * 但是, 根据服务器关于事务资源缓存的规则, 考虑将此设置至少切换为{@link #CACHE_CONNECTION} 或 {@link #CACHE_SESSION},
	 * 即使与外部事务管理器一起使用也是如此.
	 */
	public void setCacheLevel(int cacheLevel) {
		this.cacheLevel = cacheLevel;
	}

	/**
	 * 返回允许此监听器容器应用的缓存级别.
	 */
	public int getCacheLevel() {
		return this.cacheLevel;
	}


	/**
	 * 通过"下限-上限"字符串指定并发限制, e.g. "5-10", 或简单的上限字符串, e.g. "10" (在这种情况下, 下限为1).
	 * <p>此侦听器容器将始终保持最少数量的消费者 ({@link #setConcurrentConsumers}),
	 * 并且在负载增加的情况下将慢慢扩展到最大数量的消费者 {@link #setMaxConcurrentConsumers}.
	 */
	@Override
	public void setConcurrency(String concurrency) {
		try {
			int separatorIndex = concurrency.indexOf('-');
			if (separatorIndex != -1) {
				setConcurrentConsumers(Integer.parseInt(concurrency.substring(0, separatorIndex)));
				setMaxConcurrentConsumers(Integer.parseInt(concurrency.substring(separatorIndex + 1, concurrency.length())));
			}
			else {
				setConcurrentConsumers(1);
				setMaxConcurrentConsumers(Integer.parseInt(concurrency));
			}
		}
		catch (NumberFormatException ex) {
			throw new IllegalArgumentException("Invalid concurrency value [" + concurrency + "]: only " +
					"single maximum integer (e.g. \"5\") and minimum-maximum combo (e.g. \"3-5\") supported.");
		}
	}

	/**
	 * 指定要创建的并发消费者数量. 默认 1.
	 * <p>为此设置指定更高的值, 将在运行时增加已调度的并发消费者的标准级别:
	 * 这实际上是在任何给定时间安排的最小并发消费者数量.
	 * 这是一个静态设置; 对于动态缩放, 考虑指定"maxConcurrentConsumers"设置.
	 * <p>建议增加并发消费者的数量, 以便扩展从队列进入的消息的消费.
	 * 但请注意, 一旦多个消费者注册, 任何顺序保证都会丢失.
	 * 一般来说, 坚持使用1个消费者来进行低容量队列.
	 * <p><b>除非特定于供应商的设置措施明确允许, 否则不要增加topic的并发消费者数量.</b>
	 * 通过常规设置, 这将导致同时消费相同的消息, 这几乎是不可取的.
	 * <p><b>可以在运行时修改此设置, 例如通过JMX.</b>
	 */
	public void setConcurrentConsumers(int concurrentConsumers) {
		Assert.isTrue(concurrentConsumers > 0, "'concurrentConsumers' value must be at least 1 (one)");
		synchronized (this.lifecycleMonitor) {
			this.concurrentConsumers = concurrentConsumers;
			if (this.maxConcurrentConsumers < concurrentConsumers) {
				this.maxConcurrentConsumers = concurrentConsumers;
			}
		}
	}

	/**
	 * 返回"concurrentConsumer"设置.
	 * <p>这将返回当前配置的"concurrentConsumers"值; 当前调度/活跃的消费者数量可能不同.
	 */
	public final int getConcurrentConsumers() {
		synchronized (this.lifecycleMonitor) {
			return this.concurrentConsumers;
		}
	}

	/**
	 * 指定要创建的最大并发消费者数量. 默认 1.
	 * <p>如果此设置高于"concurrentConsumers", 则监听器容器将在运行时动态调度新的使用者, 前提是遇到足够的传入消息.
	 * 一旦负载再次下降, 消费者的数量将再次降低到标准水平 ("concurrentConsumers").
	 * <p>建议增加并发消费者的数量, 以便扩展从队列进入的消息的消费.
	 * 但请注意, 一旦多个消费者注册, 任何顺序保证都会丢失.
	 * 一般来说, 坚持使用1个消费者来进行低容量队列.
	 * <p><b>除非特定于供应商的设置措施明确允许, 否则不要增加topic的并发消费者数量.</b>
	 * 通过常规设置, 这将导致同时消费相同的消息, 这几乎是不可取的.
	 * <p><b>可以在运行时修改此设置, 例如通过JMX.</b>
	 */
	public void setMaxConcurrentConsumers(int maxConcurrentConsumers) {
		Assert.isTrue(maxConcurrentConsumers > 0, "'maxConcurrentConsumers' value must be at least 1 (one)");
		synchronized (this.lifecycleMonitor) {
			this.maxConcurrentConsumers =
					(maxConcurrentConsumers > this.concurrentConsumers ? maxConcurrentConsumers : this.concurrentConsumers);
		}
	}

	/**
	 * 返回"maxConcurrentConsumer"设置.
	 * <p>这将返回当前配置的"maxConcurrentConsumers"值; 当前调度/活跃的消费者数量可能不同.
	 */
	public final int getMaxConcurrentConsumers() {
		synchronized (this.lifecycleMonitor) {
			return this.maxConcurrentConsumers;
		}
	}

	/**
	 * 指定一个任务中要处理的最大消息数.
	 * 更具体地说, 这限制了每个任务的消息接收尝试次数, 其中包括在超时之前没有实际接收消息的接收迭代
	 * (请参阅{@link #setReceiveTimeout "receiveTimeout"}属性).
	 * <p>对于标准的TaskExecutor, 默认值为不限制 (-1), 重用原始调用器线程直到关闭 (以有限的动态调度为代价).
	 * <p>如果SchedulingTaskExecutor指示短期任务的首选项, 则默认值为10.
	 * 指定10到100条消息的数量, 以便在此处长期任务和短期任务之间进行平衡.
	 * <p>长期任务通过坚持使用相同的线程来避免频繁的线程上下文切换, 而短期任务允许线程池控制调度.
	 * 因此, 线程池通常更喜欢短期任务.
	 * <p><b>可以在运行时修改此设置, 例如通过JMX.</b>
	 */
	public void setMaxMessagesPerTask(int maxMessagesPerTask) {
		Assert.isTrue(maxMessagesPerTask != 0, "'maxMessagesPerTask' must not be 0");
		synchronized (this.lifecycleMonitor) {
			this.maxMessagesPerTask = maxMessagesPerTask;
		}
	}

	/**
	 * 返回一个任务中要处理的最大消息数.
	 */
	public final int getMaxMessagesPerTask() {
		synchronized (this.lifecycleMonitor) {
			return this.maxMessagesPerTask;
		}
	}

	/**
	 * 指定允许空闲的消费者数量的限制.
	 * <p>{@link #scheduleNewInvokerIfAppropriate}方法使用此限制来确定是否应创建新的调用器.
	 * 增加限制会导致更积极地创建调用器. 这可以更快地提高调用器的数量.
	 * <p>默认 1, 如果现有调用器当前都没有空闲, 则只调度新的调用器 (最初可能是空闲的).
	 */
	public void setIdleConsumerLimit(int idleConsumerLimit) {
		Assert.isTrue(idleConsumerLimit > 0, "'idleConsumerLimit' must be 1 or higher");
		synchronized (this.lifecycleMonitor) {
			this.idleConsumerLimit = idleConsumerLimit;
		}
	}

	/**
	 * 返回空闲消费者数量的限制.
	 */
	public final int getIdleConsumerLimit() {
		synchronized (this.lifecycleMonitor) {
			return this.idleConsumerLimit;
		}
	}

	/**
	 * 指定消费者任务的空闲执行限制, 但未在其执行中收到任何消息.
	 * 如果达到此限制, 任务将关闭并将接收留给其他正在执行的任务.
	 * <p>默认 1, 一旦任务未收到消息, 就会提前关闭空闲资源.
	 * 这仅适用于动态调度; 请参阅 {@link #setMaxConcurrentConsumers "maxConcurrentConsumers"}设置.
	 * 消费者的最小数量 (see {@link #setConcurrentConsumers "concurrentConsumers"}) 将保持不变, 直到关闭.
	 * <p>在每个任务执行期间, 许多消息接收尝试 (根据"maxMessagesPerTask"设置) 将等待传入消息(根据"receiveTimeout"设置).
	 * 如果给定任务中的所有接收尝试返回时都没有消息, 则该任务被认为是空闲的.
	 * 这项任务仍可以重新调度; 但是, 一旦达到指定的"idleTaskExecutionLimit", 它将关闭 (在动态缩放的情况下).
	 * <p>如果遇到过于频繁的放大和缩小, 请提高此限制.
	 * 随着此限制越来越高, 空闲的消费者将被保持更长时间, 一旦新的消息加载进入, 就避免重新启动消费者.
	 * 或者, 指定更高的"maxMessagesPerTask" 和/或 "receiveTimeout"值, 这也将导致闲置的消费者被保留更长时间
	 * (同时还增加了每个计划任务的平均执行时间).
	 * <p><b>可以在运行时修改此设置, 例如通过JMX.</b>
	 */
	public void setIdleTaskExecutionLimit(int idleTaskExecutionLimit) {
		Assert.isTrue(idleTaskExecutionLimit > 0, "'idleTaskExecutionLimit' must be 1 or higher");
		synchronized (this.lifecycleMonitor) {
			this.idleTaskExecutionLimit = idleTaskExecutionLimit;
		}
	}

	/**
	 * 返回消费者任务的空闲执行限制.
	 */
	public final int getIdleTaskExecutionLimit() {
		synchronized (this.lifecycleMonitor) {
			return this.idleTaskExecutionLimit;
		}
	}


	//-------------------------------------------------------------------------
	// Implementation of AbstractMessageListenerContainer's template methods
	//-------------------------------------------------------------------------

	@Override
	public void initialize() {
		// 调整默认缓存级别.
		if (this.cacheLevel == CACHE_AUTO) {
			this.cacheLevel = (getTransactionManager() != null ? CACHE_NONE : CACHE_CONSUMER);
		}

		// 准备taskExecutor 和 maxMessagesPerTask.
		synchronized (this.lifecycleMonitor) {
			if (this.taskExecutor == null) {
				this.taskExecutor = createDefaultTaskExecutor();
			}
			else if (this.taskExecutor instanceof SchedulingTaskExecutor &&
					((SchedulingTaskExecutor) this.taskExecutor).prefersShortLivedTasks() &&
					this.maxMessagesPerTask == Integer.MIN_VALUE) {
				// TaskExecutor指出了对短期任务的偏好.
				// 根据 setMaxMessagesPerTask javadoc, 在这种情况下将为每个任务使用10条消息, 除非用户指定了自定义值.
				this.maxMessagesPerTask = 10;
			}
		}

		// 继续进行实际的监听器初始化.
		super.initialize();
	}

	/**
	 * 创建指定数量的并发消费者, 以JMS会话的形式, 加上在单独线程中运行的关联的MessageConsumer.
	 */
	@Override
	protected void doInitialize() throws JMSException {
		synchronized (this.lifecycleMonitor) {
			for (int i = 0; i < this.concurrentConsumers; i++) {
				scheduleNewInvoker();
			}
		}
	}

	/**
	 * 销毁已注册的JMS会话和关联的MessageConsumer.
	 */
	@Override
	protected void doShutdown() throws JMSException {
		logger.debug("Waiting for shutdown of message listener invokers");
		try {
			synchronized (this.lifecycleMonitor) {
				int waitCount = 0;
				while (this.activeInvokerCount > 0) {
					if (logger.isDebugEnabled()) {
						logger.debug("Still waiting for shutdown of " + this.activeInvokerCount +
								" message listener invokers (iteration " + waitCount + ")");
					}
					// 等待AsyncMessageListenerInvokers停用它们自己...
					long timeout = getReceiveTimeout();
					if (timeout > 0) {
						this.lifecycleMonitor.wait(timeout);
					}
					else {
						this.lifecycleMonitor.wait();
					}
					waitCount++;
				}
				// 清除剩余的计划调用器, 可能会暂停作为暂停的任务
				for (AsyncMessageListenerInvoker scheduledInvoker : this.scheduledInvokers) {
					scheduledInvoker.clearResources();
				}
				this.scheduledInvokers.clear();
			}
		}
		catch (InterruptedException ex) {
			// 重新中断当前线程, 以允许其他线程做出反应.
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * 重写以重置停止回调.
	 */
	@Override
	public void start() throws JmsException {
		synchronized (this.lifecycleMonitor) {
			this.stopCallback = null;
		}
		super.start();
	}

	/**
	 * 停止此监听器容器, 一旦所有监听器处理实际停止, 就调用特定的回调.
	 * <p>Note: 进一步的{@code stop(runnable)}调用 (在处理实际停止之前) 将覆盖指定的回调.
	 * 仅调用最新指定的回调.
	 * <p>如果后续的{@link #start()}调用在监听器容器完全停止之前重新启动监听器容器, 则根本不会调用回调.
	 * 
	 * @param callback 一旦监听器处理完全停止就调用的回调
	 * 
	 * @throws JmsException 停止失败
	 */
	@Override
	public void stop(Runnable callback) throws JmsException {
		synchronized (this.lifecycleMonitor) {
			if (!isRunning() || this.stopCallback != null) {
				// 未启动, 已停止, 或先前停止尝试正在进行  -> 立即返回.
				callback.run();
				return;
			}
			this.stopCallback = callback;
		}
		stop();
	}

	/**
	 * 返回当前调度的消费者数量.
	 * <p>此数量将始终在"concurrentConsumers" 和 "maxConcurrentConsumers"之间, 但可能高于"activeConsumerCount"
	 * (如果某些消费者已调度但目前尚未执行).
	 */
	public final int getScheduledConsumerCount() {
		synchronized (this.lifecycleMonitor) {
			return this.scheduledInvokers.size();
		}
	}

	/**
	 * 返回当前活动的消费者数量.
	 * <p>此数量将始终在"concurrentConsumers" 和 "maxConcurrentConsumers", 但可能低于"scheduledConsumerCount"
	 * (如果某些消费者已调度但目前尚未执行).
	 */
	public final int getActiveConsumerCount() {
		synchronized (this.lifecycleMonitor) {
			return this.activeInvokerCount;
		}
	}

	/**
	 * 返回消费者是否已进入目标的固定注册.
	 * 对于pub-sub情况来说, 这一点尤为有趣, 因为注册的实际消费者可能很重要, 保证不会错过任何即将发布的消息.
	 * <p>可以在{@link #start()}调用之后轮询此方法, 直到消费者的异步注册发生, 这时方法将开始返回{@code true} &ndash;
	 * 只要监听器容器实际上建立了固定的注册.
	 * 然后它将继续返回{@code true}直到关闭, 因为容器将在此后保持至少一个消费者注册.
	 * <p>请注意, 监听器容器不一定要首先进行固定注册.
	 * 它还可以为每个调用者执行重建消费者.
	 * 这尤其取决于{@link #setCacheLevel 缓存级别}设置:
	 * 只有{@link #CACHE_CONSUMER}才会导致固定注册.
	 */
	public boolean isRegisteredWithDestination() {
		synchronized (this.lifecycleMonitor) {
			return (this.registeredWithDestination > 0);
		}
	}


	/**
	 * 创建一个默认的TaskExecutor. 如果未指定显式的TaskExecutor, 则调用.
	 * <p>默认实现使用指定的bean名称(或类名, 如果未指定bean名称)构建
	 * {@link org.springframework.core.task.SimpleAsyncTaskExecutor}作为线程名称前缀.
	 */
	protected TaskExecutor createDefaultTaskExecutor() {
		String beanName = getBeanName();
		String threadNamePrefix = (beanName != null ? beanName + "-" : DEFAULT_THREAD_NAME_PREFIX);
		return new SimpleAsyncTaskExecutor(threadNamePrefix);
	}

	/**
	 * 调度新的调用者, 增加此监听器容器的计划调用器总数.
	 */
	private void scheduleNewInvoker() {
		AsyncMessageListenerInvoker invoker = new AsyncMessageListenerInvoker();
		if (rescheduleTaskIfNecessary(invoker)) {
			// 这应该总是 true, 因为只在活动时调用它.
			this.scheduledInvokers.add(invoker);
		}
	}

	/**
	 * 根据"cacheLevel"设置使用共享JMS连接.
	 */
	@Override
	protected final boolean sharedConnectionEnabled() {
		return (getCacheLevel() >= CACHE_CONNECTION);
	}

	/**
	 * 通过此监听器容器的TaskExecutor重新执行给定任务.
	 */
	@Override
	protected void doRescheduleTask(Object task) {
		this.taskExecutor.execute((Runnable) task);
	}

	/**
	 * 尝试调度一个新的调用者, 因为知道消息正在进入...
	 */
	@Override
	protected void messageReceived(Object invoker, Session session) {
		((AsyncMessageListenerInvoker) invoker).setIdle(false);
		scheduleNewInvokerIfAppropriate();
	}

	/**
	 * 将受影响的调用者标记为空闲.
	 */
	@Override
	protected void noMessageReceived(Object invoker, Session session) {
		((AsyncMessageListenerInvoker) invoker).setIdle(true);
	}

	/**
	 * 调度一个新的调用者, 增加该监听器容器的计划调用器总数, 但前提是尚未达到指定的"maxConcurrentConsumers"限制,
	 * 并且仅当尚未达到指定的"idleConsumerLimit"时.
	 * <p>一旦收到消息就调用, 以便在最初接收消息的调用器中处理消息时进行扩展.
	 */
	protected void scheduleNewInvokerIfAppropriate() {
		if (isRunning()) {
			resumePausedTasks();
			synchronized (this.lifecycleMonitor) {
				if (this.scheduledInvokers.size() < this.maxConcurrentConsumers &&
						getIdleInvokerCount() < this.idleConsumerLimit) {
					scheduleNewInvoker();
					if (logger.isDebugEnabled()) {
						logger.debug("Raised scheduled invoker count: " + this.scheduledInvokers.size());
					}
				}
			}
		}
	}

	/**
	 * 确定当前调用器是否应该重新调度, 因为它可能暂时没有收到消息.
	 * 
	 * @param idleTaskExecutionCount 此调用器任务已累积的空闲执行次数 (连续)
	 */
	private boolean shouldRescheduleInvoker(int idleTaskExecutionCount) {
		boolean superfluous =
				(idleTaskExecutionCount >= this.idleTaskExecutionLimit && getIdleInvokerCount() > 1);
		return (this.scheduledInvokers.size() <=
				(superfluous ? this.concurrentConsumers : this.maxConcurrentConsumers));
	}

	/**
	 * 确定此监听器容器当前是否在其调度的调用器中具有多个空闲实例.
	 */
	private int getIdleInvokerCount() {
		int count = 0;
		for (AsyncMessageListenerInvoker invoker : this.scheduledInvokers) {
			if (invoker.isIdle()) {
				count++;
			}
		}
		return count;
	}


	/**
	 * 重写以接受初始设置中的失败 - 将其留给异步调用器以在首次访问时建立共享连接.
	 */
	@Override
	protected void establishSharedConnection() {
		try {
			super.establishSharedConnection();
		}
		catch (Exception ex) {
			if (ex instanceof JMSException) {
				invokeExceptionListener((JMSException) ex);
			}
			logger.debug("Could not establish shared JMS Connection - " +
					"leaving it up to asynchronous invokers to establish a Connection as soon as possible", ex);
		}
	}

	/**
	 * 即使在{@code Connection.start()}抛出异常之后, 此实现仍会继续, 依赖于监听器执行适当的恢复.
	 */
	@Override
	protected void startSharedConnection() {
		try {
			super.startSharedConnection();
		}
		catch (Exception ex) {
			logger.debug("Connection start failed - relying on listeners to perform recovery", ex);
		}
	}

	/**
	 * 即使在从{@code Connection.stop()}抛出异常之后, 此实现仍在继续, 依赖于监听器在重新启动后执行适当的恢复.
	 */
	@Override
	protected void stopSharedConnection() {
		try {
			super.stopSharedConnection();
		}
		catch (Exception ex) {
			logger.debug("Connection stop failed - relying on listeners to perform recovery after restart", ex);
		}
	}

	/**
	 * 处理在设置监听器期间出现的给定异常.
	 * 为每个并发监听器中的异常调用.
	 * <p>如果尚未恢复, 则默认实现将异常记录在警告级别, 如果已经恢复, 则在调试级别记录异常.
	 * 可以在子类中重写.
	 * 
	 * @param ex 要处理的异常
	 * @param alreadyRecovered 先前正在执行的监听器是否已从当前监听器设置失败中恢复
	 * (这通常表示后续失败, 除了用于调试日志之外可以忽略)
	 */
	protected void handleListenerSetupFailure(Throwable ex, boolean alreadyRecovered) {
		if (ex instanceof JMSException) {
			invokeExceptionListener((JMSException) ex);
		}
		if (ex instanceof SharedConnectionNotInitializedException) {
			if (!alreadyRecovered) {
				logger.info("JMS message listener invoker needs to establish shared Connection");
			}
		}
		else {
			// 在活动操作期间恢复..
			if (alreadyRecovered) {
				logger.debug("Setup of JMS message listener invoker failed - already recovered by other invoker", ex);
			}
			else {
				StringBuilder msg = new StringBuilder();
				msg.append("Setup of JMS message listener invoker failed for destination '");
				msg.append(getDestinationDescription()).append("' - trying to recover. Cause: ");
				msg.append(ex instanceof JMSException ? JmsUtils.buildExceptionMessage((JMSException) ex) : ex.getMessage());
				if (logger.isDebugEnabled()) {
					logger.warn(msg, ex);
				}
				else {
					logger.warn(msg);
				}
			}
		}
	}

	/**
	 * 在监听器未能自行设置之后恢复此监听器容器, 例如重新建立底层Connection.
	 * <p>默认实现委托给 DefaultMessageListenerContainer的具有恢复功能的{@link #refreshConnectionUntilSuccessful()}方法,
	 * 该方法将尝试为共享和非共享连接情况重新建立与JMS提供器的连接.
	 */
	protected void recoverAfterListenerSetupFailure() {
		this.recovering = true;
		try {
			refreshConnectionUntilSuccessful();
			refreshDestination();
		}
		finally {
			this.recovering = false;
			this.interrupted = false;
		}
	}

	/**
	 * 刷新底层Connection, 在尝试成功之前不返回.
	 * 在共享连接以及没有共享连接的情况下调用, 因此要么需要在共享连接上运行, 要么在刚刚建立用于验证目的的临时连接上运行.
	 * <p>只要此消息监听器容器正在运行, 默认实现将重试, 直到成功建立Connection.
	 * 在重试之间应用指定的恢复间隔.
	 */
	protected void refreshConnectionUntilSuccessful() {
		BackOffExecution execution = this.backOff.start();
		while (isRunning()) {
			try {
				if (sharedConnectionEnabled()) {
					refreshSharedConnection();
				}
				else {
					Connection con = createConnection();
					JmsUtils.closeConnection(con);
				}
				logger.info("Successfully refreshed JMS Connection");
				break;
			}
			catch (Exception ex) {
				if (ex instanceof JMSException) {
					invokeExceptionListener((JMSException) ex);
				}
				StringBuilder msg = new StringBuilder();
				msg.append("Could not refresh JMS Connection for destination '");
				msg.append(getDestinationDescription()).append("' - retrying using ");
				msg.append(execution).append(". Cause: ");
				msg.append(ex instanceof JMSException ? JmsUtils.buildExceptionMessage((JMSException) ex) : ex.getMessage());
				if (logger.isDebugEnabled()) {
					logger.error(msg, ex);
				}
				else {
					logger.error(msg);
				}
			}
			if (!applyBackOffTime(execution)) {
				StringBuilder msg = new StringBuilder();
				msg.append("Stopping container for destination '")
						.append(getDestinationDescription())
						.append("': back-off policy does not allow ").append("for further attempts.");
				logger.error(msg.toString());
				stop();
			}
		}
	}

	/**
	 * 刷新此监听器容器操作的JMS目标.
	 * <p>在监听器设置失败后调用, 假设缓存的Destination对象可能已变为无效 (WebLogic JMS上的典型情况).
	 * <p>在CachingDestinationResolver的情况下, 默认实现从DestinationResolver的缓存中删除目标.
	 */
	protected void refreshDestination() {
		String destName = getDestinationName();
		if (destName != null) {
			DestinationResolver destResolver = getDestinationResolver();
			if (destResolver instanceof CachingDestinationResolver) {
				((CachingDestinationResolver) destResolver).removeFromCache(destName);
			}
		}
	}

	/**
	 * 使用指定的{@link BackOffExecution}应用下一个退避时间.
	 * <p>如果已经应用了退避间隔并且应该进行新的恢复尝试, 则返回{@code true}, 如果不再进行进一步的尝试, 则返回{@code false}.
	 */
	protected boolean applyBackOffTime(BackOffExecution execution) {
		if (this.recovering && this.interrupted) {
			// 之前中断但仍然失败... 放弃.
			return false;
		}
		long interval = execution.nextBackOff();
		if (interval == BackOffExecution.STOP) {
			return false;
		}
		else {
			try {
				synchronized (this.lifecycleMonitor) {
					this.lifecycleMonitor.wait(interval);
				}
			}
			catch (InterruptedException interEx) {
				// 重新中断当前线程, 以允许其他线程做出反应.
				Thread.currentThread().interrupt();
				if (this.recovering) {
					this.interrupted = true;
				}
			}
			return true;
		}
	}

	/**
	 * 返回此监听器容器当前是否处于恢复尝试中.
	 * <p>可用于检测恢复阶段, 但也可用于恢复阶段的结束,
	 * 在找到返回{@code true}之后{@code isRecovering()}切换到{@code false}.
	 */
	public final boolean isRecovering() {
		return this.recovering;
	}


	//-------------------------------------------------------------------------
	// Inner classes used as internal adapters
	//-------------------------------------------------------------------------

	/**
	 * 执行循环{@code MessageConsumer.receive()}调用的Runnable.
	 */
	private class AsyncMessageListenerInvoker implements SchedulingAwareRunnable {

		private Session session;

		private MessageConsumer consumer;

		private Object lastRecoveryMarker;

		private boolean lastMessageSucceeded;

		private int idleTaskExecutionCount = 0;

		private volatile boolean idle = true;

		@Override
		public void run() {
			synchronized (lifecycleMonitor) {
				activeInvokerCount++;
				lifecycleMonitor.notifyAll();
			}
			boolean messageReceived = false;
			try {
				if (maxMessagesPerTask < 0) {
					messageReceived = executeOngoingLoop();
				}
				else {
					int messageCount = 0;
					while (isRunning() && messageCount < maxMessagesPerTask) {
						messageReceived = (invokeListener() || messageReceived);
						messageCount++;
					}
				}
			}
			catch (Throwable ex) {
				clearResources();
				if (!this.lastMessageSucceeded) {
					// 连续多次或在启动时失败 - 在第一次恢复尝试之前等待.
					waitBeforeRecoveryAttempt();
				}
				this.lastMessageSucceeded = false;
				boolean alreadyRecovered = false;
				synchronized (recoveryMonitor) {
					if (this.lastRecoveryMarker == currentRecoveryMarker) {
						handleListenerSetupFailure(ex, false);
						recoverAfterListenerSetupFailure();
						currentRecoveryMarker = new Object();
					}
					else {
						alreadyRecovered = true;
					}
				}
				if (alreadyRecovered) {
					handleListenerSetupFailure(ex, true);
				}
			}
			finally {
				synchronized (lifecycleMonitor) {
					decreaseActiveInvokerCount();
					lifecycleMonitor.notifyAll();
				}
				if (!messageReceived) {
					this.idleTaskExecutionCount++;
				}
				else {
					this.idleTaskExecutionCount = 0;
				}
				synchronized (lifecycleMonitor) {
					if (!shouldRescheduleInvoker(this.idleTaskExecutionCount) || !rescheduleTaskIfNecessary(this)) {
						// 完全关闭.
						scheduledInvokers.remove(this);
						if (logger.isDebugEnabled()) {
							logger.debug("Lowered scheduled invoker count: " + scheduledInvokers.size());
						}
						lifecycleMonitor.notifyAll();
						clearResources();
					}
					else if (isRunning()) {
						int nonPausedConsumers = getScheduledConsumerCount() - getPausedTaskCount();
						if (nonPausedConsumers < 1) {
							logger.error("All scheduled consumers have been paused, probably due to tasks having been rejected. " +
									"Check your thread pool configuration! Manual recovery necessary through a start() call.");
						}
						else if (nonPausedConsumers < getConcurrentConsumers()) {
							logger.warn("Number of scheduled consumers has dropped below concurrentConsumers limit, probably " +
									"due to tasks having been rejected. Check your thread pool configuration! Automatic recovery " +
									"to be triggered by remaining consumers.");
						}
					}
				}
			}
		}

		private boolean executeOngoingLoop() throws JMSException {
			boolean messageReceived = false;
			boolean active = true;
			while (active) {
				synchronized (lifecycleMonitor) {
					boolean interrupted = false;
					boolean wasWaiting = false;
					while ((active = isActive()) && !isRunning()) {
						if (interrupted) {
							throw new IllegalStateException("Thread was interrupted while waiting for " +
									"a restart of the listener container, but container is still stopped");
						}
						if (!wasWaiting) {
							decreaseActiveInvokerCount();
						}
						wasWaiting = true;
						try {
							lifecycleMonitor.wait();
						}
						catch (InterruptedException ex) {
							// 重新中断当前线程, 以允许其他线程做出反应.
							Thread.currentThread().interrupt();
							interrupted = true;
						}
					}
					if (wasWaiting) {
						activeInvokerCount++;
					}
					if (scheduledInvokers.size() > maxConcurrentConsumers) {
						active = false;
					}
				}
				if (active) {
					messageReceived = (invokeListener() || messageReceived);
				}
			}
			return messageReceived;
		}

		private boolean invokeListener() throws JMSException {
			initResourcesIfNecessary();
			boolean messageReceived = receiveAndExecute(this, this.session, this.consumer);
			this.lastMessageSucceeded = true;
			return messageReceived;
		}

		private void decreaseActiveInvokerCount() {
			activeInvokerCount--;
			if (stopCallback != null && activeInvokerCount == 0) {
				stopCallback.run();
				stopCallback = null;
			}
		}

		private void initResourcesIfNecessary() throws JMSException {
			if (getCacheLevel() <= CACHE_CONNECTION) {
				updateRecoveryMarker();
			}
			else {
				if (this.session == null && getCacheLevel() >= CACHE_SESSION) {
					updateRecoveryMarker();
					this.session = createSession(getSharedConnection());
				}
				if (this.consumer == null && getCacheLevel() >= CACHE_CONSUMER) {
					this.consumer = createListenerConsumer(this.session);
					synchronized (lifecycleMonitor) {
						registeredWithDestination++;
					}
				}
			}
		}

		private void updateRecoveryMarker() {
			synchronized (recoveryMonitor) {
				this.lastRecoveryMarker = currentRecoveryMarker;
			}
		}

		private void clearResources() {
			if (sharedConnectionEnabled()) {
				synchronized (sharedConnectionMonitor) {
					JmsUtils.closeMessageConsumer(this.consumer);
					JmsUtils.closeSession(this.session);
				}
			}
			else {
				JmsUtils.closeMessageConsumer(this.consumer);
				JmsUtils.closeSession(this.session);
			}
			if (this.consumer != null) {
				synchronized (lifecycleMonitor) {
					registeredWithDestination--;
				}
			}
			this.consumer = null;
			this.session = null;
		}

		/**
		 * 应用退避时间一次.
		 * 在常规情况下, 仅当无法使用代理恢复时才会应用退避.
		 * 这个额外的等待时间避免了当代理实际启动时的突发重试场景, 但是如果失败则避免其他情况 (i.e. 特定于监听器).
		 */
		private void waitBeforeRecoveryAttempt() {
			BackOffExecution execution = DefaultMessageListenerContainer.this.backOff.start();
			applyBackOffTime(execution);
		}

		@Override
		public boolean isLongLived() {
			return (maxMessagesPerTask < 0);
		}

		public void setIdle(boolean idle) {
			this.idle = idle;
		}

		public boolean isIdle() {
			return this.idle;
		}
	}
}
