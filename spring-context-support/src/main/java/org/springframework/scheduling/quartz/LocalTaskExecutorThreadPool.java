package org.springframework.scheduling.quartz;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.SchedulerConfigException;
import org.quartz.spi.ThreadPool;

/**
 * Quartz ThreadPool适配器, 它委托给在SchedulerFactoryBean上指定的Spring管理的TaskExecutor实例.
 */
public class LocalTaskExecutorThreadPool implements ThreadPool {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private Executor taskExecutor;


	@Override
	public void setInstanceId(String schedInstId) {
	}

	@Override
	public void setInstanceName(String schedName) {
	}


	@Override
	public void initialize() throws SchedulerConfigException {
		// 绝对需要线程绑定的TaskExecutor来初始化.
		this.taskExecutor = SchedulerFactoryBean.getConfigTimeTaskExecutor();
		if (this.taskExecutor == null) {
			throw new SchedulerConfigException(
				"No local TaskExecutor found for configuration - " +
				"'taskExecutor' property must be set on SchedulerFactoryBean");
		}
	}

	@Override
	public void shutdown(boolean waitForJobsToComplete) {
	}

	@Override
	public int getPoolSize() {
		return -1;
	}


	@Override
	public boolean runInThread(Runnable runnable) {
		if (runnable == null) {
			return false;
		}
		try {
			this.taskExecutor.execute(runnable);
			return true;
		}
		catch (RejectedExecutionException ex) {
			logger.error("Task has been rejected by TaskExecutor", ex);
			return false;
		}
	}

	@Override
	public int blockForAvailableThreads() {
		// 当前的实现总是返回1, 这使得Quartz始终调度任何感觉像调度的任务.
		// 对于特定的TaskExecutors, 这可以更聪明,
		// 例如在{@code java.util.concurrent.ThreadPoolExecutor}上调用{@code getMaximumPoolSize() - getActiveCount()}.
		return 1;
	}

}
