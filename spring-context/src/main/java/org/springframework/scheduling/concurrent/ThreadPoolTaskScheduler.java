package org.springframework.scheduling.concurrent;

import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.lang.UsesJava7;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.TaskUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ErrorHandler;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureTask;

/**
 * Spring的{@link TaskScheduler}接口的实现, 包装了一个原生的{@link java.util.concurrent.ScheduledThreadPoolExecutor}.
 */
@SuppressWarnings("serial")
public class ThreadPoolTaskScheduler extends ExecutorConfigurationSupport
		implements AsyncListenableTaskExecutor, SchedulingTaskExecutor, TaskScheduler {

	// ScheduledThreadPoolExecutor.setRemoveOnCancelPolicy(boolean) only available on JDK 7+
	private static final boolean setRemoveOnCancelPolicyAvailable =
			ClassUtils.hasMethod(ScheduledThreadPoolExecutor.class, "setRemoveOnCancelPolicy", boolean.class);


	private volatile int poolSize = 1;

	private volatile boolean removeOnCancelPolicy = false;

	private volatile ErrorHandler errorHandler;

	private volatile ScheduledExecutorService scheduledExecutor;


	/**
	 * 设置ScheduledExecutorService的池大小.
	 * 默认 1.
	 * <p><b>可以在运行时修改此设置, 例如通过JMX.</b>
	 */
	public void setPoolSize(int poolSize) {
		Assert.isTrue(poolSize > 0, "'poolSize' must be 1 or higher");
		this.poolSize = poolSize;
		if (this.scheduledExecutor instanceof ScheduledThreadPoolExecutor) {
			((ScheduledThreadPoolExecutor) this.scheduledExecutor).setCorePoolSize(poolSize);
		}
	}

	/**
	 * 在{@link ScheduledThreadPoolExecutor}上设置remove-on-cancel模式 (JDK 7+).
	 * <p>默认 {@code false}.
	 * 如果设置为{@code true}, 目标执行器将切换到remove-on-cancel模式 (如果可能, 否则将使用软回退).
	 * <p><b>可以在运行时修改此设置, 例如通过JMX.</b>
	 */
	@UsesJava7
	public void setRemoveOnCancelPolicy(boolean removeOnCancelPolicy) {
		this.removeOnCancelPolicy = removeOnCancelPolicy;
		if (setRemoveOnCancelPolicyAvailable && this.scheduledExecutor instanceof ScheduledThreadPoolExecutor) {
			((ScheduledThreadPoolExecutor) this.scheduledExecutor).setRemoveOnCancelPolicy(removeOnCancelPolicy);
		}
		else if (removeOnCancelPolicy && this.scheduledExecutor != null) {
			logger.info("Could not apply remove-on-cancel policy - not a Java 7+ ScheduledThreadPoolExecutor");
		}
	}

	/**
	 * 设置自定义{@link ErrorHandler}策略.
	 */
	public void setErrorHandler(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}


	@UsesJava7
	@Override
	protected ExecutorService initializeExecutor(
			ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {

		this.scheduledExecutor = createExecutor(this.poolSize, threadFactory, rejectedExecutionHandler);

		if (this.removeOnCancelPolicy) {
			if (setRemoveOnCancelPolicyAvailable && this.scheduledExecutor instanceof ScheduledThreadPoolExecutor) {
				((ScheduledThreadPoolExecutor) this.scheduledExecutor).setRemoveOnCancelPolicy(true);
			}
			else {
				logger.info("Could not apply remove-on-cancel policy - not a Java 7+ ScheduledThreadPoolExecutor");
			}
		}

		return this.scheduledExecutor;
	}

	/**
	 * 创建{@link ScheduledExecutorService}实例.
	 * <p>默认实现创建{@link ScheduledThreadPoolExecutor}.
	 * 可以在子类中重写以提供自定义的{@link ScheduledExecutorService}实例.
	 * 
	 * @param poolSize 指定的池大小
	 * @param threadFactory 要使用的ThreadFactory
	 * @param rejectedExecutionHandler 要使用的RejectedExecutionHandler
	 * 
	 * @return 新的ScheduledExecutorService实例
	 */
	protected ScheduledExecutorService createExecutor(
			int poolSize, ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {

		return new ScheduledThreadPoolExecutor(poolSize, threadFactory, rejectedExecutionHandler);
	}

	/**
	 * 返回底层ScheduledExecutorService以进行本机访问.
	 * 
	 * @return 底层ScheduledExecutorService (never {@code null})
	 * @throws IllegalStateException 如果ThreadPoolTask​​Scheduler尚未初始化
	 */
	public ScheduledExecutorService getScheduledExecutor() throws IllegalStateException {
		Assert.state(this.scheduledExecutor != null, "ThreadPoolTaskScheduler not initialized");
		return this.scheduledExecutor;
	}

	/**
	 * 返回底层ScheduledThreadPoolExecutor.
	 * 
	 * @return 底层ScheduledExecutorService (never {@code null})
	 * @throws IllegalStateException 如果ThreadPoolTask​​Scheduler尚未初始化,
	 * 或者如果底层ScheduledExecutorService不是ScheduledThreadPoolExecutor
	 */
	public ScheduledThreadPoolExecutor getScheduledThreadPoolExecutor() throws IllegalStateException {
		Assert.state(this.scheduledExecutor instanceof ScheduledThreadPoolExecutor,
				"No ScheduledThreadPoolExecutor available");
		return (ScheduledThreadPoolExecutor) this.scheduledExecutor;
	}

	/**
	 * 返回当前池大小.
	 * <p>需要底层{@link ScheduledThreadPoolExecutor}.
	 */
	public int getPoolSize() {
		if (this.scheduledExecutor == null) {
			// 尚未初始化: 假设初始池大小.
			return this.poolSize;
		}
		return getScheduledThreadPoolExecutor().getPoolSize();
	}

	/**
	 * 返回remove-on-cancel模式的当前设置.
	 * <p>需要底层{@link ScheduledThreadPoolExecutor}.
	 */
	@UsesJava7
	public boolean isRemoveOnCancelPolicy() {
		if (!setRemoveOnCancelPolicyAvailable) {
			return false;
		}
		if (this.scheduledExecutor == null) {
			// 尚未初始化: 暂时返回我们的设置.
			return this.removeOnCancelPolicy;
		}
		return getScheduledThreadPoolExecutor().getRemoveOnCancelPolicy();
	}

	/**
	 * 返回当前活动线程的数量.
	 * <p>需要底层{@link ScheduledThreadPoolExecutor}.
	 */
	public int getActiveCount() {
		if (this.scheduledExecutor == null) {
			// 尚未初始化: 假设没有活动线程.
			return 0;
		}
		return getScheduledThreadPoolExecutor().getActiveCount();
	}


	// SchedulingTaskExecutor implementation

	@Override
	public void execute(Runnable task) {
		Executor executor = getScheduledExecutor();
		try {
			executor.execute(errorHandlingTask(task, false));
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
		ExecutorService executor = getScheduledExecutor();
		try {
			return executor.submit(errorHandlingTask(task, false));
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public <T> Future<T> submit(Callable<T> task) {
		ExecutorService executor = getScheduledExecutor();
		try {
			Callable<T> taskToUse = task;
			if (this.errorHandler != null) {
				taskToUse = new DelegatingErrorHandlingCallable<T>(task, this.errorHandler);
			}
			return executor.submit(taskToUse);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public ListenableFuture<?> submitListenable(Runnable task) {
		ExecutorService executor = getScheduledExecutor();
		try {
			ListenableFutureTask<Object> future = new ListenableFutureTask<Object>(task, null);
			executor.execute(errorHandlingTask(future, false));
			return future;
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public <T> ListenableFuture<T> submitListenable(Callable<T> task) {
		ExecutorService executor = getScheduledExecutor();
		try {
			ListenableFutureTask<T> future = new ListenableFutureTask<T>(task);
			executor.execute(errorHandlingTask(future, false));
			return future;
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public boolean prefersShortLivedTasks() {
		return true;
	}


	// TaskScheduler implementation

	@Override
	public ScheduledFuture<?> schedule(Runnable task, Trigger trigger) {
		ScheduledExecutorService executor = getScheduledExecutor();
		try {
			ErrorHandler errorHandler =
					(this.errorHandler != null ? this.errorHandler : TaskUtils.getDefaultErrorHandler(true));
			return new ReschedulingRunnable(task, trigger, executor, errorHandler).schedule();
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public ScheduledFuture<?> schedule(Runnable task, Date startTime) {
		ScheduledExecutorService executor = getScheduledExecutor();
		long initialDelay = startTime.getTime() - System.currentTimeMillis();
		try {
			return executor.schedule(errorHandlingTask(task, false), initialDelay, TimeUnit.MILLISECONDS);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Date startTime, long period) {
		ScheduledExecutorService executor = getScheduledExecutor();
		long initialDelay = startTime.getTime() - System.currentTimeMillis();
		try {
			return executor.scheduleAtFixedRate(errorHandlingTask(task, true), initialDelay, period, TimeUnit.MILLISECONDS);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long period) {
		ScheduledExecutorService executor = getScheduledExecutor();
		try {
			return executor.scheduleAtFixedRate(errorHandlingTask(task, true), 0, period, TimeUnit.MILLISECONDS);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Date startTime, long delay) {
		ScheduledExecutorService executor = getScheduledExecutor();
		long initialDelay = startTime.getTime() - System.currentTimeMillis();
		try {
			return executor.scheduleWithFixedDelay(errorHandlingTask(task, true), initialDelay, delay, TimeUnit.MILLISECONDS);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long delay) {
		ScheduledExecutorService executor = getScheduledExecutor();
		try {
			return executor.scheduleWithFixedDelay(errorHandlingTask(task, true), 0, delay, TimeUnit.MILLISECONDS);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
		}
	}


	private Runnable errorHandlingTask(Runnable task, boolean isRepeatingTask) {
		return TaskUtils.decorateTaskWithErrorHandler(task, this.errorHandler, isRepeatingTask);
	}


	private static class DelegatingErrorHandlingCallable<V> implements Callable<V> {

		private final Callable<V> delegate;

		private final ErrorHandler errorHandler;

		public DelegatingErrorHandlingCallable(Callable<V> delegate, ErrorHandler errorHandler) {
			this.delegate = delegate;
			this.errorHandler = errorHandler;
		}

		@Override
		public V call() throws Exception {
			try {
				return this.delegate.call();
			}
			catch (Throwable t) {
				this.errorHandler.handleError(t);
				return null;
			}
		}
	}
}
