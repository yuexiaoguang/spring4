package org.springframework.core.task;

import java.io.Serializable;

import org.springframework.util.Assert;

/**
 * {@link TaskExecutor}实现, 在调用线程中<i>同步</i>执行任务.
 *
 * <p>主要用于测试场景.
 *
 * <p>调用线程中的执行确实具有参与其线程上下文的优点, 例如线程上下文类加载器或线程的当前事务关联.
 * 也就是说, 在许多情况下, 异步执行将更为可取: 在这种情况下, 选择异步{@code TaskExecutor}.
 */
@SuppressWarnings("serial")
public class SyncTaskExecutor implements TaskExecutor, Serializable {

	/**
	 * 同步执行给定的{@code task}, 通过直接调用{@link Runnable#run() run()}方法.
	 * 
	 * @throws IllegalArgumentException 如果给定的{@code task}是{@code null}
	 */
	@Override
	public void execute(Runnable task) {
		Assert.notNull(task, "Runnable must not be null");
		task.run();
	}
}
