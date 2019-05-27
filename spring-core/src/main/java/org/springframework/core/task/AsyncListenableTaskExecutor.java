package org.springframework.core.task;

import java.util.concurrent.Callable;

import org.springframework.util.concurrent.ListenableFuture;

/**
 * {@link AsyncTaskExecutor}接口的扩展, 添加为{@link ListenableFuture}提交任务的功能.
 */
public interface AsyncListenableTaskExecutor extends AsyncTaskExecutor {

	/**
	 * 提交要执行的{@code Runnable}任务, 接收代表该任务的{@code ListenableFuture}.
	 * Future将在完成后返回{@code null}结果.
	 * 
	 * @param task 要执行的{@code Runnable} (never {@code null})
	 * 
	 * @return 一个代表待完成任务的{@code ListenableFuture}
	 * @throws TaskRejectedException 如果给定的任务未被接受
	 */
	ListenableFuture<?> submitListenable(Runnable task);

	/**
	 * 提交要执行的{@code Callable}任务, 接收代表该任务的{@code ListenableFuture}.
	 * Future将在完成后返回Callable的结果.
	 * 
	 * @param task 要执行的{@code Callable} (never {@code null})
	 * 
	 * @return 一个代表待完成任务的{@code ListenableFuture}
	 * @throws TaskRejectedException 如果给定的任务未被接受
	 */
	<T> ListenableFuture<T> submitListenable(Callable<T> task);

}
