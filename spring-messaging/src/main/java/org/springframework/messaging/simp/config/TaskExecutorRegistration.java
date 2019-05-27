package org.springframework.messaging.simp.config;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.Assert;

/**
 * 用于自定义{@link ThreadPoolTask​​Executor}属性的注册类.
 */
public class TaskExecutorRegistration {

	private final ThreadPoolTaskExecutor taskExecutor;

	private Integer corePoolSize;

	private Integer maxPoolSize;

	private Integer keepAliveSeconds;

	private Integer queueCapacity;


	/**
	 * 用于默认的{@link ThreadPoolTaskExecutor}.
	 */
	public TaskExecutorRegistration() {
		this.taskExecutor = new ThreadPoolTaskExecutor();
		this.taskExecutor.setCorePoolSize(Runtime.getRuntime().availableProcessors() * 2);
		this.taskExecutor.setAllowCoreThreadTimeOut(true);
	}

	/**
	 * 用于给定的{@link ThreadPoolTaskExecutor}.
	 * 
	 * @param taskExecutor 要使用的执行器
	 */
	public TaskExecutorRegistration(ThreadPoolTaskExecutor taskExecutor) {
		Assert.notNull(taskExecutor, "ThreadPoolTaskExecutor must not be null");
		this.taskExecutor = taskExecutor;
	}


	/**
	 * 设置ThreadPoolExecutor的核心池大小.
	 * <p><strong>NOTE:</strong> 当配置了无界{@link #queueCapacity(int) queueCapacity}时 (默认值), 核心池大小实际上是最大池大小.
	 * 这基本上是"无界队列"策略, 如{@link java.util.concurrent.ThreadPoolExecutor ThreadPoolExecutor}中所述.
	 * 使用此策略时, 将忽略{@link #maxPoolSize(int) maxPoolSize}.
	 * <p>默认设置为{@link Runtime#availableProcessors()}值的两倍.
	 * 在任务不经常阻塞的应用程序中, 该数字应该接近或等于可用CPU/核心数.
	 */
	public TaskExecutorRegistration corePoolSize(int corePoolSize) {
		this.corePoolSize = corePoolSize;
		return this;
	}

	/**
	 * 设置ThreadPoolExecutor的最大池大小.
	 * <p><strong>NOTE:</strong> 如果配置了无界{@link #queueCapacity(int) queueCapacity} (默认值), 则会有效地忽略最大池大小.
	 * 有关详细信息, 请参阅{@link java.util.concurrent.ThreadPoolExecutor ThreadPoolExecutor}中的"无界队列"策略.
	 * <p>默认{@code Integer.MAX_VALUE}.
	 */
	public TaskExecutorRegistration maxPoolSize(int maxPoolSize) {
		this.maxPoolSize = maxPoolSize;
		return this;
	}

	/**
	 * 设置线程在终止之前可以保持空闲的时间限制.
	 * 如果池中当前有多个线程核心数, 则在等待这段时间而不处理任务后, 将终止多余的线程.
	 * 这将覆盖构造函数中设置的任何值.
	 * <p>默认 60.
	 */
	public TaskExecutorRegistration keepAliveSeconds(int keepAliveSeconds) {
		this.keepAliveSeconds = keepAliveSeconds;
		return this;
	}

	/**
	 * 设置ThreadPoolExecutor的队列容量.
	 * <p><strong>NOTE:</strong> 当配置无界{@code queueCapacity} (默认值)时, 核心池大小实际上是最大池大小.
	 * 这基本上是"无界队列"策略, 如{@link java.util.concurrent.ThreadPoolExecutor ThreadPoolExecutor}中所述.
	 * 使用此策略时, 将忽略{@link #maxPoolSize(int) maxPoolSize}.
	 * <p>默认 {@code Integer.MAX_VALUE}.
	 */
	public TaskExecutorRegistration queueCapacity(int queueCapacity) {
		this.queueCapacity = queueCapacity;
		return this;
	}


	protected ThreadPoolTaskExecutor getTaskExecutor() {
		if (this.corePoolSize != null) {
			this.taskExecutor.setCorePoolSize(this.corePoolSize);
		}
		if (this.maxPoolSize != null) {
			this.taskExecutor.setMaxPoolSize(this.maxPoolSize);
		}
		if (this.keepAliveSeconds != null) {
			this.taskExecutor.setKeepAliveSeconds(this.keepAliveSeconds);
		}
		if (this.queueCapacity != null) {
			this.taskExecutor.setQueueCapacity(this.queueCapacity);
		}
		return this.taskExecutor;
	}

}
