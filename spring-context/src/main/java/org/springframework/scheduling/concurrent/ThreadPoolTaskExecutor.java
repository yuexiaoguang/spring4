package org.springframework.scheduling.concurrent;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureTask;

/**
 * 允许以bean风格配置{@link java.util.concurrent.ThreadPoolExecutor}的JavaBean
 * (通过它的 "corePoolSize", "maxPoolSize", "keepAliveSeconds", "queueCapacity"属性)
 * 并将其暴露为Spring {@link org.springframework.core.task.TaskExecutor}.
 * 该类也非常适合管理和监视 (e.g. 通过 JMX), 提供了一些有用的属性:
 * "corePoolSize", "maxPoolSize", "keepAliveSeconds" (所有支持运行时的更新); "poolSize", "activeCount" (仅用于内省).
 *
 * <p>默认配置是核心池大小为1, 具有无限的最大池大小和无限的队列容量.
 * 这大致相当于{@link java.util.concurrent.Executors#newSingleThreadExecutor()}, 所有任务共享单个线程.
 * 将{@link #setQueueCapacity "queueCapacity"}设置为0, 模仿{@link java.util.concurrent.Executors#newCachedThreadPool()},
 * 立即将池中的线程扩展为可能非常高的数字.
 * 考虑在那时设置{@link #setMaxPoolSize "maxPoolSize"}, 以及可能更高的{@link #setCorePoolSize "corePoolSize"}
 * (另见{@link #setAllowCoreThreadTimeOut "allowCoreThreadTimeOut"}缩放模式).
 *
 * <p><b>NOTE:</b> 这个类实现了Spring的{@link org.springframework.core.task.TaskExecutor}接口
 * 以及{@link java.util.concurrent.Executor}接口, 前者是主要的接口, 另一个只是作为次要的便利.
 * 因此, 异常处理遵循TaskExecutor约定, 而不是Executor约定,
 * 特别是关于{@link org.springframework.core.task.TaskRejectedException}.
 *
 * <p>或者, 可以使用构造函数注入直接设置ThreadPoolExecutor实例,
 * 或使用指向{@link java.util.concurrent.Executors}类的工厂方法定义.
 * 将这样的原始Executor暴露为Spring {@link org.springframework.core.task.TaskExecutor},
 * 只需用{@link org.springframework.scheduling.concurrent.ConcurrentTaskExecutor}适配器包装它.
 */
@SuppressWarnings("serial")
public class ThreadPoolTaskExecutor extends ExecutorConfigurationSupport
		implements AsyncListenableTaskExecutor, SchedulingTaskExecutor {

	private final Object poolSizeMonitor = new Object();

	private int corePoolSize = 1;

	private int maxPoolSize = Integer.MAX_VALUE;

	private int keepAliveSeconds = 60;

	private int queueCapacity = Integer.MAX_VALUE;

	private boolean allowCoreThreadTimeOut = false;

	private TaskDecorator taskDecorator;

	private ThreadPoolExecutor threadPoolExecutor;


	/**
	 * 设置ThreadPoolExecutor的核心池大小.
	 * 默认 1.
	 * <p><b>可以在运行时修改此设置, 例如通过JMX.</b>
	 */
	public void setCorePoolSize(int corePoolSize) {
		synchronized (this.poolSizeMonitor) {
			this.corePoolSize = corePoolSize;
			if (this.threadPoolExecutor != null) {
				this.threadPoolExecutor.setCorePoolSize(corePoolSize);
			}
		}
	}

	/**
	 * 返回ThreadPoolExecutor的核心池大小.
	 */
	public int getCorePoolSize() {
		synchronized (this.poolSizeMonitor) {
			return this.corePoolSize;
		}
	}

	/**
	 * 设置ThreadPoolExecutor的最大池大小.
	 * 默认{@code Integer.MAX_VALUE}.
	 * <p><b>可以在运行时修改此设置, 例如通过JMX.</b>
	 */
	public void setMaxPoolSize(int maxPoolSize) {
		synchronized (this.poolSizeMonitor) {
			this.maxPoolSize = maxPoolSize;
			if (this.threadPoolExecutor != null) {
				this.threadPoolExecutor.setMaximumPoolSize(maxPoolSize);
			}
		}
	}

	/**
	 * 返回ThreadPoolExecutor的最大池大小.
	 */
	public int getMaxPoolSize() {
		synchronized (this.poolSizeMonitor) {
			return this.maxPoolSize;
		}
	}

	/**
	 * 设置ThreadPoolExecutor的 keep-alive秒数.
	 * 默认 60.
	 * <p><b>可以在运行时修改此设置, 例如通过JMX.</b>
	 */
	public void setKeepAliveSeconds(int keepAliveSeconds) {
		synchronized (this.poolSizeMonitor) {
			this.keepAliveSeconds = keepAliveSeconds;
			if (this.threadPoolExecutor != null) {
				this.threadPoolExecutor.setKeepAliveTime(keepAliveSeconds, TimeUnit.SECONDS);
			}
		}
	}

	/**
	 * 返回ThreadPoolExecutor的 keep-alive秒数.
	 */
	public int getKeepAliveSeconds() {
		synchronized (this.poolSizeMonitor) {
			return this.keepAliveSeconds;
		}
	}

	/**
	 * 设置ThreadPoolExecutor的BlockingQueue的容量.
	 * 默认 {@code Integer.MAX_VALUE}.
	 * <p>正值都将导致LinkedBlockingQueue实例;
	 * 其他值都将导致SynchronousQueue实例.
	 */
	public void setQueueCapacity(int queueCapacity) {
		this.queueCapacity = queueCapacity;
	}

	/**
	 * 指定是否允许核心线程超时.
	 * 即使与非零队列结合, 也可以实现动态增长和收缩 (因为一旦队列满, 最大池大小才会增长).
	 * <p>默认 "false".
	 */
	public void setAllowCoreThreadTimeOut(boolean allowCoreThreadTimeOut) {
		this.allowCoreThreadTimeOut = allowCoreThreadTimeOut;
	}

	/**
	 * 指定要应用于即将执行的{@link Runnable}的自定义{@link TaskDecorator}.
	 * <p>请注意, 这样的装饰器不一定应用于用户提供的{@code Runnable}/{@code Callable}, 而是应用于实际的执行回调
	 * (它可能是用户提供的任务的包装器).
	 * <p>主要用例是围绕任务的调用设置一些执行上下文, 或者为任务执行提供一些监视/统计.
	 */
	public void setTaskDecorator(TaskDecorator taskDecorator) {
		this.taskDecorator = taskDecorator;
	}


	/**
	 * Note: 此方法将{@link ExecutorService}公开给其基类, 但在内部存储实际的{@link ThreadPoolExecutor}句柄.
	 * 不要覆盖此方法来替换执行器, 而只是用于装饰其{@code ExecutorService}句柄或存储自定义状态.
	 */
	@Override
	protected ExecutorService initializeExecutor(
			ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {

		BlockingQueue<Runnable> queue = createQueue(this.queueCapacity);

		ThreadPoolExecutor executor;
		if (this.taskDecorator != null) {
			executor = new ThreadPoolExecutor(
					this.corePoolSize, this.maxPoolSize, this.keepAliveSeconds, TimeUnit.SECONDS,
					queue, threadFactory, rejectedExecutionHandler) {
				@Override
				public void execute(Runnable command) {
					super.execute(taskDecorator.decorate(command));
				}
			};
		}
		else {
			executor = new ThreadPoolExecutor(
					this.corePoolSize, this.maxPoolSize, this.keepAliveSeconds, TimeUnit.SECONDS,
					queue, threadFactory, rejectedExecutionHandler);

		}

		if (this.allowCoreThreadTimeOut) {
			executor.allowCoreThreadTimeOut(true);
		}

		this.threadPoolExecutor = executor;
		return executor;
	}

	/**
	 * 创建BlockingQueue以用于ThreadPoolExecutor.
	 * <p>将为正容量值创建LinkedBlockingQueue实例; 否则SynchronousQueue.
	 * 
	 * @param queueCapacity 指定的队列容量
	 * 
	 * @return BlockingQueue实例
	 */
	protected BlockingQueue<Runnable> createQueue(int queueCapacity) {
		if (queueCapacity > 0) {
			return new LinkedBlockingQueue<Runnable>(queueCapacity);
		}
		else {
			return new SynchronousQueue<Runnable>();
		}
	}

	/**
	 * 返回底层的ThreadPoolExecutor以进行本机访问.
	 * 
	 * @return 底层的ThreadPoolExecutor (never {@code null})
	 * @throws IllegalStateException 如果ThreadPoolTask​​Executor尚未初始化
	 */
	public ThreadPoolExecutor getThreadPoolExecutor() throws IllegalStateException {
		Assert.state(this.threadPoolExecutor != null, "ThreadPoolTaskExecutor not initialized");
		return this.threadPoolExecutor;
	}

	/**
	 * 返回当前池大小.
	 */
	public int getPoolSize() {
		if (this.threadPoolExecutor == null) {
			// 尚未初始化: 假设核心池大小.
			return this.corePoolSize;
		}
		return this.threadPoolExecutor.getPoolSize();
	}

	/**
	 * 返回当前活动的线程的数量.
	 */
	public int getActiveCount() {
		if (this.threadPoolExecutor == null) {
			// 尚未初始化: 假设没有活动线程.
			return 0;
		}
		return this.threadPoolExecutor.getActiveCount();
	}


	@Override
	public void execute(Runnable task) {
		Executor executor = getThreadPoolExecutor();
		try {
			executor.execute(task);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public void execute(Runnable task, long startTimeout) {
		execute(task);
	}

	@Override
	public Future<?> submit(Runnable task) {
		ExecutorService executor = getThreadPoolExecutor();
		try {
			return executor.submit(task);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public <T> Future<T> submit(Callable<T> task) {
		ExecutorService executor = getThreadPoolExecutor();
		try {
			return executor.submit(task);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public ListenableFuture<?> submitListenable(Runnable task) {
		ExecutorService executor = getThreadPoolExecutor();
		try {
			ListenableFutureTask<Object> future = new ListenableFutureTask<Object>(task, null);
			executor.execute(future);
			return future;
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public <T> ListenableFuture<T> submitListenable(Callable<T> task) {
		ExecutorService executor = getThreadPoolExecutor();
		try {
			ListenableFutureTask<T> future = new ListenableFutureTask<T>(task);
			executor.execute(future);
			return future;
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
		}
	}

	/**
	 * 该任务执行器更喜欢短时间运行的工作单元.
	 */
	@Override
	public boolean prefersShortLivedTasks() {
		return true;
	}

}
