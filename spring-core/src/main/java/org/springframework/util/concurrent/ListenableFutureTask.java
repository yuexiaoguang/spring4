package org.springframework.util.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 * 实现{@link ListenableFuture}的{@link FutureTask}的扩展.
 */
public class ListenableFutureTask<T> extends FutureTask<T> implements ListenableFuture<T> {

	private final ListenableFutureCallbackRegistry<T> callbacks = new ListenableFutureCallbackRegistry<T>();


	/**
	 * 创建一个新的{@code ListenableFutureTask}, 在运行时执行给定的{@link Callable}.
	 * 
	 * @param callable 回调任务
	 */
	public ListenableFutureTask(Callable<T> callable) {
		super(callable);
	}

	/**
	 * 创建一个{@code ListenableFutureTask}, 在运行时执行给定的{@link Runnable},
	 * 并安排{@link #get()}在成功完成时返回给定的结果.
	 * 
	 * @param runnable 可运行的任务
	 * @param result 成功完成后返回的结果
	 */
	public ListenableFutureTask(Runnable runnable, T result) {
		super(runnable, result);
	}


	@Override
	public void addCallback(ListenableFutureCallback<? super T> callback) {
		this.callbacks.addCallback(callback);
	}

	@Override
	public void addCallback(SuccessCallback<? super T> successCallback, FailureCallback failureCallback) {
		this.callbacks.addSuccessCallback(successCallback);
		this.callbacks.addFailureCallback(failureCallback);
	}


	@Override
	protected void done() {
		Throwable cause;
		try {
			T result = get();
			this.callbacks.success(result);
			return;
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			return;
		}
		catch (ExecutionException ex) {
			cause = ex.getCause();
			if (cause == null) {
				cause = ex;
			}
		}
		catch (Throwable ex) {
			cause = ex;
		}
		this.callbacks.failure(cause);
	}

}
