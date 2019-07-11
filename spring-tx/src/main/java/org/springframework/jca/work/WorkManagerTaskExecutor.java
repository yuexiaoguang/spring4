package org.springframework.jca.work;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import javax.naming.NamingException;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.work.ExecutionContext;
import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkListener;
import javax.resource.spi.work.WorkManager;
import javax.resource.spi.work.WorkRejectedException;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.core.task.TaskTimeoutException;
import org.springframework.jca.context.BootstrapContextAware;
import org.springframework.jndi.JndiLocatorSupport;
import org.springframework.scheduling.SchedulingException;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureTask;

/**
 * 委托给JCA 1.5 WorkManager的{@link org.springframework.core.task.TaskExecutor}实现,
 * 实现{@link javax.resource.spi.work.WorkManager}接口.
 *
 * <p>这主要用于JCA ResourceAdapter实现, 但也可以在独立环境中使用, 委托给本地嵌入的WorkManager实现 (例如 Geronimo's).
 *
 * <p>还实现了JCA 1.5 WorkManager接口本身, 将所有调用委托给目标WorkManager.
 * 因此, 调用者可以通过Spring TaskExecutor接口或JCA 1.5 WorkManager接口选择是否要与此执行器通信.
 *
 * <p>此适配器还能够从JNDI获取JCA WorkManager.
 * 例如, 这适用于Geronimo应用程序服务器, 其中WorkManager GBeans (e.g. Geronimo的默认"DefaultWorkManager" GBean)
 * 可以通过{@code geronimo-web.xml}部署描述符中的"gbean-ref"条目链接到J2EE环境中.
 *
 * <p><b>在JBoss和GlassFish上, 获取默认的JCA WorkManager需要特殊的查找步骤.</b>
 * 请参阅{@link org.springframework.jca.work.jboss.JBossWorkManagerTaskExecutor}
 * {@link org.springframework.jca.work.glassfish.GlassFishWorkManagerTaskExecutor}类,
 * 它们直接等效于此通用JCA适配器类.
 */
public class WorkManagerTaskExecutor extends JndiLocatorSupport
		implements AsyncListenableTaskExecutor, SchedulingTaskExecutor, WorkManager, BootstrapContextAware, InitializingBean {

	private WorkManager workManager;

	private String workManagerName;

	private boolean blockUntilStarted = false;

	private boolean blockUntilCompleted = false;

	private WorkListener workListener;

	private TaskDecorator taskDecorator;


	public WorkManagerTaskExecutor() {
	}

	/**
	 * @param workManager 要委托的JCA WorkManager
	 */
	public WorkManagerTaskExecutor(WorkManager workManager) {
		setWorkManager(workManager);
	}


	/**
	 * 指定要委托的JCA WorkManager实例.
	 */
	public void setWorkManager(WorkManager workManager) {
		Assert.notNull(workManager, "WorkManager must not be null");
		this.workManager = workManager;
	}

	/**
	 * 设置JCA WorkManager的JNDI名称.
	 * <p>如果"resourceRef"设置为"true", 则可以是完全限定的JNDI名称, 也可以是相对于当前环境命名上下文的JNDI名称.
	 */
	public void setWorkManagerName(String workManagerName) {
		this.workManagerName = workManagerName;
	}

	/**
	 * 指定包含要委托给的WorkManager的JCA BootstrapContext.
	 */
	@Override
	public void setBootstrapContext(BootstrapContext bootstrapContext) {
		Assert.notNull(bootstrapContext, "BootstrapContext must not be null");
		this.workManager = bootstrapContext.getWorkManager();
	}

	/**
	 * 设置是否让{@link #execute}阻塞, 直到工作实际开始.
	 * <p>使用下面的JCA {@code startWork}操作, 而不是默认的{@code scheduleWork}.
	 */
	public void setBlockUntilStarted(boolean blockUntilStarted) {
		this.blockUntilStarted = blockUntilStarted;
	}

	/**
	 * 设置是否让{@link #execute}阻塞, 直到工作完成.
	 * <p>使用下面的JCA {@code doWork}操作, 而不是默认的{@code scheduleWork}.
	 */
	public void setBlockUntilCompleted(boolean blockUntilCompleted) {
		this.blockUntilCompleted = blockUntilCompleted;
	}

	/**
	 * 指定要应用的JCA 1.5 WorkListener.
	 * <p>这个共享的WorkListener实例将通过此TaskExecutor上的所有{@link #execute}调用传递给WorkManager.
	 */
	public void setWorkListener(WorkListener workListener) {
		this.workListener = workListener;
	}

	/**
	 * 指定要应用于即将执行的任何{@link Runnable}的自定义{@link TaskDecorator}.
	 * <p>请注意, 这样的装饰器不一定应用于用户提供的{@code Runnable}/{@code Callable},
	 * 而是应用于实际执行回调 (可能是用户提供的任务的包装器).
	 * <p>主要用例是围绕任务的调用设置一些执行上下文, 或者为任务执行提供一些监视/统计.
	 */
	public void setTaskDecorator(TaskDecorator taskDecorator) {
		this.taskDecorator = taskDecorator;
	}

	@Override
	public void afterPropertiesSet() throws NamingException {
		if (this.workManager == null) {
			if (this.workManagerName != null) {
				this.workManager = lookup(this.workManagerName, WorkManager.class);
			}
			else {
				this.workManager = getDefaultWorkManager();
			}
		}
	}

	/**
	 * 获取要委托给的默认WorkManager.
	 * 如果未指定显式的WorkManager或WorkManager JNDI名称, 则调用此方法.
	 * <p>默认实现返回{@link SimpleTaskWorkManager}. 可以在子类中重写.
	 */
	protected WorkManager getDefaultWorkManager() {
		return new SimpleTaskWorkManager();
	}


	//-------------------------------------------------------------------------
	// Implementation of the Spring SchedulingTaskExecutor interface
	//-------------------------------------------------------------------------

	@Override
	public void execute(Runnable task) {
		execute(task, TIMEOUT_INDEFINITE);
	}

	@Override
	public void execute(Runnable task, long startTimeout) {
		Assert.state(this.workManager != null, "No WorkManager specified");
		Work work = new DelegatingWork(this.taskDecorator != null ? this.taskDecorator.decorate(task) : task);
		try {
			if (this.blockUntilCompleted) {
				if (startTimeout != TIMEOUT_INDEFINITE || this.workListener != null) {
					this.workManager.doWork(work, startTimeout, null, this.workListener);
				}
				else {
					this.workManager.doWork(work);
				}
			}
			else if (this.blockUntilStarted) {
				if (startTimeout != TIMEOUT_INDEFINITE || this.workListener != null) {
					this.workManager.startWork(work, startTimeout, null, this.workListener);
				}
				else {
					this.workManager.startWork(work);
				}
			}
			else {
				if (startTimeout != TIMEOUT_INDEFINITE || this.workListener != null) {
					this.workManager.scheduleWork(work, startTimeout, null, this.workListener);
				}
				else {
					this.workManager.scheduleWork(work);
				}
			}
		}
		catch (WorkRejectedException ex) {
			if (WorkException.START_TIMED_OUT.equals(ex.getErrorCode())) {
				throw new TaskTimeoutException("JCA WorkManager rejected task because of timeout: " + task, ex);
			}
			else {
				throw new TaskRejectedException("JCA WorkManager rejected task: " + task, ex);
			}
		}
		catch (WorkException ex) {
			throw new SchedulingException("Could not schedule task on JCA WorkManager", ex);
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
	 * 该任务执行器更喜欢短期工作单位.
	 */
	@Override
	public boolean prefersShortLivedTasks() {
		return true;
	}


	//-------------------------------------------------------------------------
	// Implementation of the JCA WorkManager interface
	//-------------------------------------------------------------------------

	@Override
	public void doWork(Work work) throws WorkException {
		this.workManager.doWork(work);
	}

	@Override
	public void doWork(Work work, long delay, ExecutionContext executionContext, WorkListener workListener)
			throws WorkException {

		this.workManager.doWork(work, delay, executionContext, workListener);
	}

	@Override
	public long startWork(Work work) throws WorkException {
		return this.workManager.startWork(work);
	}

	@Override
	public long startWork(Work work, long delay, ExecutionContext executionContext, WorkListener workListener)
			throws WorkException {

		return this.workManager.startWork(work, delay, executionContext, workListener);
	}

	@Override
	public void scheduleWork(Work work) throws WorkException {
		this.workManager.scheduleWork(work);
	}

	@Override
	public void scheduleWork(Work work, long delay, ExecutionContext executionContext, WorkListener workListener)
			throws WorkException {

		this.workManager.scheduleWork(work, delay, executionContext, workListener);
	}

}
