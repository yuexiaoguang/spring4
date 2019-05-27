package org.springframework.core.task;

import java.io.Serializable;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadFactory;

import org.springframework.util.Assert;
import org.springframework.util.ConcurrencyThrottleSupport;
import org.springframework.util.CustomizableThreadCreator;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureTask;

/**
 * {@link TaskExecutor}实现, 为每个任务激活一个新线程, 异步执行它.
 *
 * <p>支持通过 "concurrencyLimit" bean属性限制并发线程.
 * 默认情况下, 并发线程数不受限制.
 *
 * <p><b>NOTE: 此实现不重用线程!</b>
 * 相反, 请考虑使用线程池TaskExecutor实现, 尤其是执行大量短期任务.
 */
@SuppressWarnings("serial")
public class SimpleAsyncTaskExecutor extends CustomizableThreadCreator
		implements AsyncListenableTaskExecutor, Serializable {

	/**
	 * 允许任意数量的并发调用: 即不要限制并发.
	 */
	public static final int UNBOUNDED_CONCURRENCY = ConcurrencyThrottleSupport.UNBOUNDED_CONCURRENCY;

	/**
	 * 切换并发'off': 即不允许任何并发调用.
	 */
	public static final int NO_CONCURRENCY = ConcurrencyThrottleSupport.NO_CONCURRENCY;


	/** 此执行器使用的内部并发限制 */
	private final ConcurrencyThrottleAdapter concurrencyThrottle = new ConcurrencyThrottleAdapter();

	private ThreadFactory threadFactory;

	private TaskDecorator taskDecorator;


	/**
	 * 使用默认线程名称前缀创建一个新的SimpleAsyncTaskExecutor.
	 */
	public SimpleAsyncTaskExecutor() {
		super();
	}

	/**
	 * @param threadNamePrefix 用于新创建的线程名称的前缀
	 */
	public SimpleAsyncTaskExecutor(String threadNamePrefix) {
		super(threadNamePrefix);
	}

	/**
	 * @param threadFactory 用于创建新线程的工厂
	 */
	public SimpleAsyncTaskExecutor(ThreadFactory threadFactory) {
		this.threadFactory = threadFactory;
	}


	/**
	 * 指定用于创建新线程的外部工厂, 而不是依赖此执行器的本地属性.
	 * <p>可以指定内部ThreadFactory bean或从JNDI(在Java EE 6服务器上)或其他一些查找机制获得的ThreadFactory引用.
	 */
	public void setThreadFactory(ThreadFactory threadFactory) {
		this.threadFactory = threadFactory;
	}

	/**
	 * 返回用于创建新线程的外部工厂.
	 */
	public final ThreadFactory getThreadFactory() {
		return this.threadFactory;
	}

	/**
	 * 指定要应用于即将执行的任何{@link Runnable}的自定义{@link TaskDecorator}.
	 * <p>请注意, 这样的装饰器不一定应用于用户提供的{@code Runnable}/{@code Callable},
	 * 而是应用于实际执行回调 (可能是用户提供的任务的包装器).
	 * <p>主要用例是围绕任务的调用设置一些执行上下文, 或者为任务执行提供一些监视/统计.
	 */
	public final void setTaskDecorator(TaskDecorator taskDecorator) {
		this.taskDecorator = taskDecorator;
	}

	/**
	 * 设置允许的最大并行访问次数.
	 * -1表示根本没有并发限制.
	 * <p>原则上, 此限制可以在运行时更改, 但通常设计为配置时间设置.
	 * NOTE: 不要在运行时在-1和任何具体限制之间切换, 因为这会导致并发计数不一致:
	 * -1的限制有效地完全关闭了并发计数.
	 */
	public void setConcurrencyLimit(int concurrencyLimit) {
		this.concurrencyThrottle.setConcurrencyLimit(concurrencyLimit);
	}

	/**
	 * 返回允许的最大并行访问数.
	 */
	public final int getConcurrencyLimit() {
		return this.concurrencyThrottle.getConcurrencyLimit();
	}

	/**
	 * 返回并发限制当前是否有效.
	 * 
	 * @return {@code true}如果此实例的并发限制有效
	 */
	public final boolean isThrottleActive() {
		return this.concurrencyThrottle.isThrottleActive();
	}


	/**
	 * 如果配置(通过超类的设置), 在并发限制内执行给定任务.
	 */
	@Override
	public void execute(Runnable task) {
		execute(task, TIMEOUT_INDEFINITE);
	}

	/**
	 * 如果配置(通过超类的设置), 在并发限制内执行给定任务.
	 * <p>直接执行紧急任务 (具有'立即'超时), 绕过并发限制.
	 * 所有其他任务都受到限制.
	 */
	@Override
	public void execute(Runnable task, long startTimeout) {
		Assert.notNull(task, "Runnable must not be null");
		Runnable taskToUse = (this.taskDecorator != null ? this.taskDecorator.decorate(task) : task);
		if (isThrottleActive() && startTimeout > TIMEOUT_IMMEDIATE) {
			this.concurrencyThrottle.beforeAccess();
			doExecute(new ConcurrencyThrottlingRunnable(taskToUse));
		}
		else {
			doExecute(taskToUse);
		}
	}

	@Override
	public Future<?> submit(Runnable task) {
		FutureTask<Object> future = new FutureTask<Object>(task, null);
		execute(future, TIMEOUT_INDEFINITE);
		return future;
	}

	@Override
	public <T> Future<T> submit(Callable<T> task) {
		FutureTask<T> future = new FutureTask<T>(task);
		execute(future, TIMEOUT_INDEFINITE);
		return future;
	}

	@Override
	public ListenableFuture<?> submitListenable(Runnable task) {
		ListenableFutureTask<Object> future = new ListenableFutureTask<Object>(task, null);
		execute(future, TIMEOUT_INDEFINITE);
		return future;
	}

	@Override
	public <T> ListenableFuture<T> submitListenable(Callable<T> task) {
		ListenableFutureTask<T> future = new ListenableFutureTask<T>(task);
		execute(future, TIMEOUT_INDEFINITE);
		return future;
	}

	/**
	 * 用于实际执行任务的模板方法.
	 * <p>默认实现创建一个新线程并启动它.
	 * 
	 * @param task 要运行的Runnable
	 */
	protected void doExecute(Runnable task) {
		Thread thread = (this.threadFactory != null ? this.threadFactory.newThread(task) : createThread(task));
		thread.start();
	}


	/**
	 * 一般ConcurrencyThrottleSupport类的子类, 使{@code beforeAccess()}和{@code afterAccess()}对周围的类可见.
	 */
	private static class ConcurrencyThrottleAdapter extends ConcurrencyThrottleSupport {

		@Override
		protected void beforeAccess() {
			super.beforeAccess();
		}

		@Override
		protected void afterAccess() {
			super.afterAccess();
		}
	}


	/**
	 * 在目标Runnable完成执行后, 此Runnable调用{@code afterAccess()}.
	 */
	private class ConcurrencyThrottlingRunnable implements Runnable {

		private final Runnable target;

		public ConcurrencyThrottlingRunnable(Runnable target) {
			this.target = target;
		}

		@Override
		public void run() {
			try {
				this.target.run();
			}
			finally {
				concurrencyThrottle.afterAccess();
			}
		}
	}
}
