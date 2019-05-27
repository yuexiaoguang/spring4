package org.springframework.core.task;

import java.util.concurrent.Executor;

/**
 * 简单的任务执行器接口, 用于抽象{@link Runnable}的执行.
 *
 * <p>实现可以使用各种不同的执行策略, 例如: 同步, 异步, 使用线程池, 等.
 *
 * <p>相当于JDK 1.5的{@link java.util.concurrent.Executor}接口;
 * 现在在Spring 3.0中扩展它, 以便客户端可以声明对Executor的依赖, 并接收任何TaskExecutor实现.
 * 此接口与标准Executor接口保持独立, 主要是为了向后兼容Spring 2.x中的JDK 1.4.
 */
public interface TaskExecutor extends Executor {

	/**
	 * 执行给定的{@code task}.
	 * <p>如果实现使用异步执行策略, 则调用可能立即返回; 或者在同步执行的情况下可能会阻塞.
	 * 
	 * @param task 要执行的{@code Runnable} (never {@code null})
	 * 
	 * @throws TaskRejectedException 如果给定的任务未被接受
	 */
	@Override
	void execute(Runnable task);

}
