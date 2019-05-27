package org.springframework.core.task;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * 异步{@link TaskExecutor}实现的扩展接口, 提供带有start timeout参数的重载{@link #execute(Runnable, long)}变体,
 * 以及对{@link java.util.concurrent.Callable}的支持.
 *
 * <p>Note: {@link java.util.concurrent.Executors}类包含一组方法, 可以在执行它们之前,
 * 将其他一些常见的类似闭包的对象, 例如, {@link java.security.PrivilegedAction} 转换为{@link Callable}.
 *
 * <p>实现此接口还表明{@link #execute(Runnable)}方法不会在调用者的线程中执行其Runnable, 而是在其他某个线程中异步执行.
 */
public interface AsyncTaskExecutor extends TaskExecutor {

	/** 表示立即执行的常量 */
	long TIMEOUT_IMMEDIATE = 0;

	/** 表示没有时间限制的常量 */
	long TIMEOUT_INDEFINITE = Long.MAX_VALUE;


	/**
	 * 执行给定的{@code task}.
	 * 
	 * @param task 要执行的{@code Runnable} (never {@code null})
	 * @param startTimeout 任务应该开始的持续时间 (毫秒).
	 * 这旨在作为执行器的提示, 允许对即时任务进行优先处理.
	 * 典型值为{@link #TIMEOUT_IMMEDIATE}或{@link #TIMEOUT_INDEFINITE} ({@link #execute(Runnable)}使用的默认值).
	 * 
	 * @throws TaskTimeoutException 如果任务因超时而被拒绝 (i.e. 无法及时启动)
	 * @throws TaskRejectedException 如果给定的任务未被接受
	 */
	void execute(Runnable task, long startTimeout);

	/**
	 * 提交要执行的Runnable任务, 接收表示该任务的Future.
	 * Future将在完成后返回{@code null}结果.
	 * 
	 * @param task 要执行的{@code Runnable} (never {@code null})
	 * 
	 * @return 表示未完成任务的Future
	 * @throws TaskRejectedException 如果给定的任务未被接受
	 */
	Future<?> submit(Runnable task);

	/**
	 * 提交可执行的Callable任务, 接收表示该任务的Future.
	 * Future将在完成后返回Callable的结果.
	 * 
	 * @param task 要执行的{@code Callable} (never {@code null})
	 * 
	 * @return 表示未完成任务的Future
	 * @throws TaskRejectedException 如果给定的任务未被接受
	 */
	<T> Future<T> submit(Callable<T> task);

}
