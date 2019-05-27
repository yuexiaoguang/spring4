package org.springframework.core.task.support;

import java.util.concurrent.Executor;

import org.springframework.core.task.TaskExecutor;
import org.springframework.util.Assert;

/**
 * 公开{@link java.util.concurrent.Executor}接口的适配器, 用于Spring {@link org.springframework.core.task.TaskExecutor}.
 *
 * <p>从Spring 3.0开始, 这不太有用, 因为TaskExecutor本身扩展了Executor接口.
 * 适配器仅与<em>隐藏</em>给定对象的TaskExecutor性质相关, 仅将标准Executor接口暴露给客户端.
 */
public class ConcurrentExecutorAdapter implements Executor {

	private final TaskExecutor taskExecutor;


	/**
	 * @param taskExecutor 要包装的Spring TaskExecutor
	 */
	public ConcurrentExecutorAdapter(TaskExecutor taskExecutor) {
		Assert.notNull(taskExecutor, "TaskExecutor must not be null");
		this.taskExecutor = taskExecutor;
	}


	@Override
	public void execute(Runnable command) {
		this.taskExecutor.execute(command);
	}
}
