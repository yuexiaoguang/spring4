package org.springframework.core.task.support;

import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.core.task.TaskExecutor;
import org.springframework.util.Assert;

/**
 * 采用Spring {@link org.springframework.core.task.TaskExecutor}
 * 并为其公开完整的{@code java.util.concurrent.ExecutorService}的适配器.
 *
 * <p>这主要是为了适配通过{@code java.util.concurrent.ExecutorService} API进行通信的客户端组件.
 * 它还可以用作Java EE 7环境中本地Spring {@code TaskExecutor}后端和位于JNDI的{@code ManagedExecutorService}之间的共同点.
 *
 * <p><b>NOTE:</b> 此ExecutorService适配器<em>不</em>支持
 * {@code java.util.concurrent.ExecutorService} API ("shutdown()" etc)中的生命周期方法,
 * 类似于Java EE 7环境中的服务器范围的{@code ManagedExecutorService}.
 * 生命周期始终由后端池决定, 此适配器充当该目标池的仅访问代理.
 */
public class ExecutorServiceAdapter extends AbstractExecutorService {

	private final TaskExecutor taskExecutor;


	/**
	 * @param taskExecutor 要委托给的目标执行器
	 */
	public ExecutorServiceAdapter(TaskExecutor taskExecutor) {
		Assert.notNull(taskExecutor, "TaskExecutor must not be null");
		this.taskExecutor = taskExecutor;
	}


	@Override
	public void execute(Runnable task) {
		this.taskExecutor.execute(task);
	}

	@Override
	public void shutdown() {
		throw new IllegalStateException(
				"Manual shutdown not supported - ExecutorServiceAdapter is dependent on an external lifecycle");
	}

	@Override
	public List<Runnable> shutdownNow() {
		throw new IllegalStateException(
				"Manual shutdown not supported - ExecutorServiceAdapter is dependent on an external lifecycle");
	}

	@Override
	public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
		throw new IllegalStateException(
				"Manual shutdown not supported - ExecutorServiceAdapter is dependent on an external lifecycle");
	}

	@Override
	public boolean isShutdown() {
		return false;
	}

	@Override
	public boolean isTerminated() {
		return false;
	}

}
