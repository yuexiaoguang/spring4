package org.springframework.core.task.support;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;

import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureTask;

/**
 * 接受JDK {@code java.util.concurrent.Executor}
 * 并为其公开Spring {@link org.springframework.core.task.TaskExecutor}的适配器.
 * 还检测扩展的{@code java.util.concurrent.ExecutorService},
 * 相应地适配{@link org.springframework.core.task.AsyncTaskExecutor}接口.
 */
public class TaskExecutorAdapter implements AsyncListenableTaskExecutor {

	private final Executor concurrentExecutor;

	private TaskDecorator taskDecorator;


	/**
	 * @param concurrentExecutor 要委托给的JDK并发执行器
	 */
	public TaskExecutorAdapter(Executor concurrentExecutor) {
		Assert.notNull(concurrentExecutor, "Executor must not be null");
		this.concurrentExecutor = concurrentExecutor;
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
	 * 委托给指定的JDK并发执行器.
	 */
	@Override
	public void execute(Runnable task) {
		try {
			doExecute(this.concurrentExecutor, this.taskDecorator, task);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException(
					"Executor [" + this.concurrentExecutor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public void execute(Runnable task, long startTimeout) {
		execute(task);
	}

	@Override
	public Future<?> submit(Runnable task) {
		try {
			if (this.taskDecorator == null && this.concurrentExecutor instanceof ExecutorService) {
				return ((ExecutorService) this.concurrentExecutor).submit(task);
			}
			else {
				FutureTask<Object> future = new FutureTask<Object>(task, null);
				doExecute(this.concurrentExecutor, this.taskDecorator, future);
				return future;
			}
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException(
					"Executor [" + this.concurrentExecutor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public <T> Future<T> submit(Callable<T> task) {
		try {
			if (this.taskDecorator == null && this.concurrentExecutor instanceof ExecutorService) {
				return ((ExecutorService) this.concurrentExecutor).submit(task);
			}
			else {
				FutureTask<T> future = new FutureTask<T>(task);
				doExecute(this.concurrentExecutor, this.taskDecorator, future);
				return future;
			}
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException(
					"Executor [" + this.concurrentExecutor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public ListenableFuture<?> submitListenable(Runnable task) {
		try {
			ListenableFutureTask<Object> future = new ListenableFutureTask<Object>(task, null);
			doExecute(this.concurrentExecutor, this.taskDecorator, future);
			return future;
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException(
					"Executor [" + this.concurrentExecutor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public <T> ListenableFuture<T> submitListenable(Callable<T> task) {
		try {
			ListenableFutureTask<T> future = new ListenableFutureTask<T>(task);
			doExecute(this.concurrentExecutor, this.taskDecorator, future);
			return future;
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException(
					"Executor [" + this.concurrentExecutor + "] did not accept task: " + task, ex);
		}
	}


	/**
	 * 实际上使用给定的执行器执行给定的{@code Runnable} (可能是用户提供的任务, 或包装用户提供的任务的包装器).
	 * 
	 * @param concurrentExecutor 要委托给的底层JDK并发执行器
	 * @param taskDecorator 要应用的指定装饰器
	 * @param runnable 要执行的runnable
	 * 
	 * @throws RejectedExecutionException 如果给定的runnable不能被接受
	 */
	protected void doExecute(Executor concurrentExecutor, TaskDecorator taskDecorator, Runnable runnable)
			throws RejectedExecutionException{

		concurrentExecutor.execute(taskDecorator != null ? taskDecorator.decorate(runnable) : runnable);
	}
}
