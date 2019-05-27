package org.springframework.jms.listener;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;

import org.springframework.jms.support.JmsUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * 消息监听器容器, 它使用普通JMS客户端API的{@code MessageConsumer.setMessageListener()}方法为指定的监听器创建并发MessageConsumer.
 *
 * <p>这是消息监听器容器的最简单形式.
 * 它创建固定数量的JMS会话以调用监听器, 而不允许动态适应运行时需求.
 * 它的主要优点是复杂程度低, 对JMS提供者的要求最低: 甚至不需要ServerSessionPool工具.
 *
 * <p>有关确认模式和事务选项的详细信息, 请参阅{@link AbstractMessageListenerContainer} javadoc.
 * 请注意, 此容器为默认的"AUTO_ACKNOWLEDGE"模式公开标准JMS行为:
 * 也就是说, 在侦听器执行后自动确认消息, 在抛出用户异常的情况下不会重新传递, 但在监听器执行期间JVM死亡时可能重新传递.
 *
 * <p>对于不同样式的MessageListener处理, 通过循环允许消息的事务性接收的{@code MessageConsumer.receive()}调用 (使用XA事务注册它们),
 * see {@link DefaultMessageListenerContainer}.
 */
public class SimpleMessageListenerContainer extends AbstractMessageListenerContainer implements ExceptionListener {

	private boolean connectLazily = false;

	private int concurrentConsumers = 1;

	private Executor taskExecutor;

	private Set<Session> sessions;

	private Set<MessageConsumer> consumers;

	private final Object consumersMonitor = new Object();


	/**
	 * 指定是否延迟连接, i.e. 是否尽可能晚地建立JMS连接和相应的Sessions和MessageConsumer - 在此容器的开始阶段.
	 * <p>默认"false": 实时连接, i.e. 在bean初始化阶段.
	 * 设置为"true", 以便切换到延迟连接, 在目标代理可能尚未启动和不尝试连接时.
	 */
	public void setConnectLazily(boolean connectLazily) {
		this.connectLazily = connectLazily;
	}

	/**
	 * 通过"下限-上限"字符串指定并发限制, e.g. "5-10", 或简单的上限字符串, e.g. "10".
	 * <p>此监听器容器将始终保持最大数量的消费者{@link #setConcurrentConsumers}, 因为它无法扩展.
	 * <p>此属性主要支持与{@link DefaultMessageListenerContainer}的配置兼容性.
	 * 对于此本地监听器容器, 通常使用{@link #setConcurrentConsumers}.
	 */
	@Override
	public void setConcurrency(String concurrency) {
		try {
			int separatorIndex = concurrency.indexOf('-');
			if (separatorIndex != -1) {
				setConcurrentConsumers(Integer.parseInt(concurrency.substring(separatorIndex + 1, concurrency.length())));
			}
			else {
				setConcurrentConsumers(Integer.parseInt(concurrency));
			}
		}
		catch (NumberFormatException ex) {
			throw new IllegalArgumentException("Invalid concurrency value [" + concurrency + "]: only " +
					"single maximum integer (e.g. \"5\") and minimum-maximum combo (e.g. \"3-5\") supported. " +
					"Note that SimpleMessageListenerContainer will effectively ignore the minimum value and " +
					"always keep a fixed number of consumers according to the maximum value.");
		}
	}

	/**
	 * 指定要创建的并发消费者数量. 默认 1.
	 * <p>建议增加并发消费者的数量, 以便扩展从队列进入的消息的消费.
	 * 但请注意, 一旦多个消费者注册, 任何顺序保证都会丢失.
	 * 一般来说, 坚持使用1个消费者来进行低容量队列.
	 * <p><b>不要为topic增加并发消费者的数量.</b>
	 * 这将导致同时消费同一消息, 这几乎是不可取的.
	 */
	public void setConcurrentConsumers(int concurrentConsumers) {
		Assert.isTrue(concurrentConsumers > 0, "'concurrentConsumers' value must be at least 1 (one)");
		this.concurrentConsumers = concurrentConsumers;
	}

	/**
	 * 设置Spring TaskExecutor, 用于在提供者收到消息后执行监听器.
	 * <p>默认无, 即在JMS提供者自己的接收线程中运行, 在执行监听器时阻塞提供者的接收端点.
	 * <p>指定TaskExecutor以在不同的线程中执行监听器, 而不是阻塞JMS提供者, 通常与现有的线程池集成.
	 * 这允许将并发消费者的数量保持在较低水平 (1), 同时仍然并发处理消息 (与接收消息分离!).
	 * <p><b>NOTE: 为监听器执行指定TaskExecutor会影响确认语义.</b>
	 * 然后, 在监听器执行之前, 将始终确认消息, 并立即重用底层会话以接收下一条消息.
	 * 将此与事务会话或客户端确认结合使用将导致未指定的结果!
	 * <p><b>NOTE: 通过TaskExecutor执行并发监听器, 将导致并发处理由相同底层会话接收的消息.</b>
	 * 因此, 不建议将此设置与{@link SessionAwareMessageListener}一起使用, 至少如果后者在给定的Session上执行实际工作则不会.
	 * 一般来说, 标准{@link javax.jms.MessageListener}可以正常工作.
	 */
	public void setTaskExecutor(Executor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	@Override
	protected void validateConfiguration() {
		super.validateConfiguration();
		if (isSubscriptionDurable() && this.concurrentConsumers != 1) {
			throw new IllegalArgumentException("Only 1 concurrent consumer supported for durable subscription");
		}
	}


	//-------------------------------------------------------------------------
	// Implementation of AbstractMessageListenerContainer's template methods
	//-------------------------------------------------------------------------

	/**
	 * 始终使用共享JMS连接.
	 */
	@Override
	protected final boolean sharedConnectionEnabled() {
		return true;
	}

	/**
	 * 以JMS Session加关联的MessageConsumer的形式创建指定数量的并发消费者.
	 */
	@Override
	protected void doInitialize() throws JMSException {
		if (!this.connectLazily) {
			try {
				establishSharedConnection();
			}
			catch (JMSException ex) {
				logger.debug("Could not connect on initialization - registering message consumers lazily", ex);
				return;
			}
			initializeConsumers();
		}
	}

	/**
	 * 如果尚未初始化, 则重新初始化此容器的JMS消息消费者.
	 */
	@Override
	protected void doStart() throws JMSException {
		super.doStart();
		initializeConsumers();
	}

	/**
	 * 在共享连接上将此监听器容器注册为JMS ExceptionListener.
	 */
	@Override
	protected void prepareSharedConnection(Connection connection) throws JMSException {
		super.prepareSharedConnection(connection);
		connection.setExceptionListener(this);
	}

	/**
	 * JMS ExceptionListener实现, 在连接失败的情况下由JMS提供者调用.
	 * 重新初始化此监听器容器的共享连接, 及其会话和消费者.
	 * 
	 * @param ex 报告的连接异常
	 */
	@Override
	public void onException(JMSException ex) {
		// 首先调用特定于用户的ExceptionListener.
		invokeExceptionListener(ex);

		// 现在尝试恢复共享Connection和所有消费者...
		if (logger.isInfoEnabled()) {
			logger.info("Trying to recover from JMS Connection exception: " + ex);
		}
		try {
			synchronized (this.consumersMonitor) {
				this.sessions = null;
				this.consumers = null;
			}
			refreshSharedConnection();
			initializeConsumers();
			logger.info("Successfully refreshed JMS Connection");
		}
		catch (JMSException recoverEx) {
			logger.debug("Failed to recover JMS Connection", recoverEx);
			logger.error("Encountered non-recoverable JMSException", ex);
		}
	}

	/**
	 * 初始化此容器的JMS Sessions和MessageConsumer.
	 * 
	 * @throws JMSException 设置失败
	 */
	protected void initializeConsumers() throws JMSException {
		// Register Sessions and MessageConsumers.
		synchronized (this.consumersMonitor) {
			if (this.consumers == null) {
				this.sessions = new HashSet<Session>(this.concurrentConsumers);
				this.consumers = new HashSet<MessageConsumer>(this.concurrentConsumers);
				Connection con = getSharedConnection();
				for (int i = 0; i < this.concurrentConsumers; i++) {
					Session session = createSession(con);
					MessageConsumer consumer = createListenerConsumer(session);
					this.sessions.add(session);
					this.consumers.add(consumer);
				}
			}
		}
	}

	/**
	 * 为给定的JMS Session创建MessageConsumer, 为指定的监听器注册MessageListener.
	 * 
	 * @param session 要使用的JMS Session
	 * 
	 * @return the MessageConsumer
	 * @throws JMSException 如果由JMS方法抛出
	 */
	protected MessageConsumer createListenerConsumer(final Session session) throws JMSException {
		Destination destination = getDestination();
		if (destination == null) {
			destination = resolveDestinationName(session, getDestinationName());
		}
		MessageConsumer consumer = createConsumer(session, destination);

		if (this.taskExecutor != null) {
			consumer.setMessageListener(new MessageListener() {
				@Override
				public void onMessage(final Message message) {
					taskExecutor.execute(new Runnable() {
						@Override
						public void run() {
							processMessage(message, session);
						}
					});
				}
			});
		}
		else {
			consumer.setMessageListener(new MessageListener() {
				@Override
				public void onMessage(Message message) {
					processMessage(message, session);
				}
			});
		}

		return consumer;
	}

	/**
	 * 处理从提供商处收到的消息.
	 * <p>执行监听器, 将当前JMS会话公开为线程绑定资源 (如果 "exposeListenerSession"为"true").
	 * 
	 * @param message 收到的JMS消息
	 * @param session 要运行的JMS会话
	 */
	protected void processMessage(Message message, Session session) {
		boolean exposeResource = isExposeListenerSession();
		if (exposeResource) {
			TransactionSynchronizationManager.bindResource(
					getConnectionFactory(), new LocallyExposedJmsResourceHolder(session));
		}
		try {
			executeListener(session, message);
		}
		finally {
			if (exposeResource) {
				TransactionSynchronizationManager.unbindResource(getConnectionFactory());
			}
		}
	}

	/**
	 * 销毁已注册的JMS会话和关联的MessageConsumer.
	 */
	@Override
	protected void doShutdown() throws JMSException {
		synchronized (this.consumersMonitor) {
			if (this.consumers != null) {
				logger.debug("Closing JMS MessageConsumers");
				for (MessageConsumer consumer : this.consumers) {
					JmsUtils.closeMessageConsumer(consumer);
				}
				logger.debug("Closing JMS Sessions");
				for (Session session : this.sessions) {
					JmsUtils.closeSession(session);
				}
			}
		}
	}

}
