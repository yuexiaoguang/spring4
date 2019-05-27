package org.springframework.scheduling.commonj;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import javax.naming.NamingException;

import commonj.work.Work;
import commonj.work.WorkException;
import commonj.work.WorkItem;
import commonj.work.WorkListener;
import commonj.work.WorkManager;
import commonj.work.WorkRejectedException;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.jndi.JndiLocatorSupport;
import org.springframework.scheduling.SchedulingException;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureTask;

/**
 * 委托给CommonJ WorkManager的TaskExecutor实现, 实现{@link commonj.work.WorkManager}接口,
 * 该接口需要指定为引用或通过JNDI名称.
 *
 * <p><b>这是在Spring上下文中设置CommonJ WorkManager的中心类.</b>
 *
 * <p>还实现了CommonJ WorkManager接口本身, 将所有调用委托给目标WorkManager.
 * 因此, 调用者可以选择是否要通过Spring TaskExecutor接口或CommonJ WorkManager接口与此执行程序通信.
 *
 * <p>CommonJ WorkManager通常将从应用程序服务器的JNDI环境中检索, 如服务器管理控制台中所定义的.
 *
 * <p>Note: 在即将推出的符合EE 7标准的WebLogic和WebSphere版本中,
 * 在Java EE 7中支持JSR-236后, 应首选{@link org.springframework.scheduling.concurrent.DefaultManagedTaskExecutor}.
 */
public class WorkManagerTaskExecutor extends JndiLocatorSupport
		implements AsyncListenableTaskExecutor, SchedulingTaskExecutor, WorkManager, InitializingBean {

	private WorkManager workManager;

	private String workManagerName;

	private WorkListener workListener;

	private TaskDecorator taskDecorator;


	/**
	 * 指定要委托的CommonJ WorkManager.
	 * <p>或者, 也可以指定目标WorkManager的JNDI名称.
	 */
	public void setWorkManager(WorkManager workManager) {
		this.workManager = workManager;
	}

	/**
	 * 设置CommonJ WorkManager的JNDI名称.
	 * <p>如果"resourceRef"设置为"true", 则可以是完全限定的JNDI名称, 也可以是相对于当前环境命名上下文的JNDI名称.
	 */
	public void setWorkManagerName(String workManagerName) {
		this.workManagerName = workManagerName;
	}

	/**
	 * 指定要应用的CommonJ WorkListener.
	 * <p>这个共享的WorkListener实例将通过此TaskExecutor上的所有{@link #execute}调用传递给WorkManager.
	 */
	public void setWorkListener(WorkListener workListener) {
		this.workListener = workListener;
	}

	/**
	 * 指定要应用于即将执行的{@link Runnable}的自定义{@link TaskDecorator}.
	 * <p>请注意, 这样的装饰器不一定应用于用户提供的{@code Runnable}/{@code Callable},
	 * 而是应用于实际执行回调(可能是用户提供的任务的包装器).
	 * <p>主要用例是围绕任务的调用设置一些执行上下文, 或者为任务执行提供一些监视/统计.
	 */
	public void setTaskDecorator(TaskDecorator taskDecorator) {
		this.taskDecorator = taskDecorator;
	}

	@Override
	public void afterPropertiesSet() throws NamingException {
		if (this.workManager == null) {
			if (this.workManagerName == null) {
				throw new IllegalArgumentException("Either 'workManager' or 'workManagerName' must be specified");
			}
			this.workManager = lookup(this.workManagerName, WorkManager.class);
		}
	}


	//-------------------------------------------------------------------------
	// Implementation of the Spring SchedulingTaskExecutor interface
	//-------------------------------------------------------------------------

	@Override
	public void execute(Runnable task) {
		Assert.state(this.workManager != null, "No WorkManager specified");
		Work work = new DelegatingWork(this.taskDecorator != null ? this.taskDecorator.decorate(task) : task);
		try {
			if (this.workListener != null) {
				this.workManager.schedule(work, this.workListener);
			}
			else {
				this.workManager.schedule(work);
			}
		}
		catch (WorkRejectedException ex) {
			throw new TaskRejectedException("CommonJ WorkManager did not accept task: " + task, ex);
		}
		catch (WorkException ex) {
			throw new SchedulingException("Could not schedule task on CommonJ WorkManager", ex);
		}
	}

	@Override
	public void execute(Runnable task, long startTimeout) {
		execute(task);
	}

	@Override
	public Future<?> submit(Runnable task) {
		FutureTask<Object> future = new FutureTask<Object>(task, null);
		execute(future);
		return future;
	}

	@Override
	public <T> Future<T> submit(Callable<T> task) {
		FutureTask<T> future = new FutureTask<T>(task);
		execute(future);
		return future;
	}

	@Override
	public ListenableFuture<?> submitListenable(Runnable task) {
		ListenableFutureTask<Object> future = new ListenableFutureTask<Object>(task, null);
		execute(future);
		return future;
	}

	@Override
	public <T> ListenableFuture<T> submitListenable(Callable<T> task) {
		ListenableFutureTask<T> future = new ListenableFutureTask<T>(task);
		execute(future);
		return future;
	}

	/**
	 * 该任务执行器更喜欢短时间运行的任务.
	 */
	@Override
	public boolean prefersShortLivedTasks() {
		return true;
	}


	//-------------------------------------------------------------------------
	// Implementation of the CommonJ WorkManager interface
	//-------------------------------------------------------------------------

	@Override
	public WorkItem schedule(Work work) throws WorkException, IllegalArgumentException {
		return this.workManager.schedule(work);
	}

	@Override
	public WorkItem schedule(Work work, WorkListener workListener) throws WorkException {
		return this.workManager.schedule(work, workListener);
	}

	@Override
	@SuppressWarnings("rawtypes")
	public boolean waitForAll(Collection workItems, long timeout) throws InterruptedException {
		return this.workManager.waitForAll(workItems, timeout);
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Collection waitForAny(Collection workItems, long timeout) throws InterruptedException {
		return this.workManager.waitForAny(workItems, timeout);
	}

}
