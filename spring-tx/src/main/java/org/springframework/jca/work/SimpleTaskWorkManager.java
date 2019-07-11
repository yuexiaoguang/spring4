package org.springframework.jca.work;

import javax.resource.spi.work.ExecutionContext;
import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkAdapter;
import javax.resource.spi.work.WorkCompletedException;
import javax.resource.spi.work.WorkEvent;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkListener;
import javax.resource.spi.work.WorkManager;
import javax.resource.spi.work.WorkRejectedException;

import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.core.task.TaskTimeoutException;
import org.springframework.util.Assert;

/**
 * 简单JCA 1.5 {@link javax.resource.spi.work.WorkManager}实现,
 * 委托给Spring {@link org.springframework.core.task.TaskExecutor}.
 * 提供简单的任务执行, 包括启动超时, 但不支持JCA ExecutionContext (i.e. 不支持导入的事务).
 *
 * <p>默认使用{@link org.springframework.core.task.SyncTaskExecutor}进行{@link #doWork}调用,
 * 使用{@link org.springframework.core.task.SimpleAsyncTaskExecutor} 进行{@link #startWork}和{@link #scheduleWork}调用.
 * 可以通过配置覆盖这些默认任务执行器.
 *
 * <p><b>NOTE: 默认情况下, 此WorkManager不提供线程池!</b>
 * 将{@link org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor} (或任何其他线程池TaskExecutor)
 * 指定为"asyncTaskExecutor"以实现实际的线程池.
 *
 * <p>此WorkManager自动检测指定的{@link org.springframework.core.task.AsyncTaskExecutor}实现,
 * 并在适当的地方使用其扩展的超时功能.
 * 无论如何都完全支持JCA WorkListener.
 */
public class SimpleTaskWorkManager implements WorkManager {

	private TaskExecutor syncTaskExecutor = new SyncTaskExecutor();

	private AsyncTaskExecutor asyncTaskExecutor = new SimpleAsyncTaskExecutor();


	/**
	 * 指定用于<i>同步</i>工作执行的TaskExecutor (i.e. {@link #doWork}调用).
	 * <p>默认{@link org.springframework.core.task.SyncTaskExecutor}.
	 */
	public void setSyncTaskExecutor(TaskExecutor syncTaskExecutor) {
		this.syncTaskExecutor = syncTaskExecutor;
	}

	/**
	 * 指定TaskExecutor用于<i>异步</i>工作执行 (i.e. {@link #startWork}和{@link #scheduleWork}调用).
	 * <p>这通常(但不一定)是{@link org.springframework.core.task.AsyncTaskExecutor}实现.
	 * 默认{@link org.springframework.core.task.SimpleAsyncTaskExecutor}.
	 */
	public void setAsyncTaskExecutor(AsyncTaskExecutor asyncTaskExecutor) {
		this.asyncTaskExecutor = asyncTaskExecutor;
	}


	@Override
	public void doWork(Work work) throws WorkException {
		doWork(work, WorkManager.INDEFINITE, null, null);
	}

	@Override
	public void doWork(Work work, long startTimeout, ExecutionContext executionContext, WorkListener workListener)
			throws WorkException {

		Assert.state(this.syncTaskExecutor != null, "No 'syncTaskExecutor' set");
		executeWork(this.syncTaskExecutor, work, startTimeout, false, executionContext, workListener);
	}

	@Override
	public long startWork(Work work) throws WorkException {
		return startWork(work, WorkManager.INDEFINITE, null, null);
	}

	@Override
	public long startWork(Work work, long startTimeout, ExecutionContext executionContext, WorkListener workListener)
			throws WorkException {

		Assert.state(this.asyncTaskExecutor != null, "No 'asyncTaskExecutor' set");
		return executeWork(this.asyncTaskExecutor, work, startTimeout, true, executionContext, workListener);
	}

	@Override
	public void scheduleWork(Work work) throws WorkException {
		scheduleWork(work, WorkManager.INDEFINITE, null, null);
	}

	@Override
	public void scheduleWork(Work work, long startTimeout, ExecutionContext executionContext, WorkListener workListener)
			throws WorkException {

		Assert.state(this.asyncTaskExecutor != null, "No 'asyncTaskExecutor' set");
		executeWork(this.asyncTaskExecutor, work, startTimeout, false, executionContext, workListener);
	}


	/**
	 * 在指定的TaskExecutor上执行给定的Work.
	 * 
	 * @param taskExecutor 要使用的TaskExecutor
	 * @param work 要执行的Work
	 * @param startTimeout Work启动的持续时间
	 * @param blockUntilStarted 是否在Work启动之前阻塞
	 * @param executionContext 给定Work的JCA ExecutionContext
	 * @param workListener 要为给定的Work进行调用的WorkListener
	 * 
	 * @return 从Work接收到开始执行所经过的时间 (如果不适用或未知, 则为-1)
	 * @throws WorkException 如果TaskExecutor不接受 Work
	 */
	protected long executeWork(TaskExecutor taskExecutor, Work work, long startTimeout,
			boolean blockUntilStarted, ExecutionContext executionContext, WorkListener workListener)
			throws WorkException {

		if (executionContext != null && executionContext.getXid() != null) {
			throw new WorkException("SimpleTaskWorkManager does not supported imported XIDs: " + executionContext.getXid());
		}
		WorkListener workListenerToUse = workListener;
		if (workListenerToUse == null) {
			workListenerToUse = new WorkAdapter();
		}

		boolean isAsync = (taskExecutor instanceof AsyncTaskExecutor);
		DelegatingWorkAdapter workHandle = new DelegatingWorkAdapter(work, workListenerToUse, !isAsync);
		try {
			if (isAsync) {
				((AsyncTaskExecutor) taskExecutor).execute(workHandle, startTimeout);
			}
			else {
				taskExecutor.execute(workHandle);
			}
		}
		catch (TaskTimeoutException ex) {
			WorkException wex = new WorkRejectedException("TaskExecutor rejected Work because of timeout: " + work, ex);
			wex.setErrorCode(WorkException.START_TIMED_OUT);
			workListenerToUse.workRejected(new WorkEvent(this, WorkEvent.WORK_REJECTED, work, wex));
			throw wex;
		}
		catch (TaskRejectedException ex) {
			WorkException wex = new WorkRejectedException("TaskExecutor rejected Work: " + work, ex);
			wex.setErrorCode(WorkException.INTERNAL);
			workListenerToUse.workRejected(new WorkEvent(this, WorkEvent.WORK_REJECTED, work, wex));
			throw wex;
		}
		catch (Throwable ex) {
			WorkException wex = new WorkException("TaskExecutor failed to execute Work: " + work, ex);
			wex.setErrorCode(WorkException.INTERNAL);
			throw wex;
		}
		if (isAsync) {
			workListenerToUse.workAccepted(new WorkEvent(this, WorkEvent.WORK_ACCEPTED, work, null));
		}

		if (blockUntilStarted) {
			long acceptanceTime = System.currentTimeMillis();
			synchronized (workHandle.monitor) {
				try {
					while (!workHandle.started) {
						workHandle.monitor.wait();
					}
				}
				catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
				}
			}
			return (System.currentTimeMillis() - acceptanceTime);
		}
		else {
			return WorkManager.UNKNOWN;
		}
	}


	/**
	 * 支持启动超时和WorkListener回调的Work适配器.
	 */
	private static class DelegatingWorkAdapter implements Work {

		private final Work work;

		private final WorkListener workListener;

		private final boolean acceptOnExecution;

		public final Object monitor = new Object();

		public boolean started = false;

		public DelegatingWorkAdapter(Work work, WorkListener workListener, boolean acceptOnExecution) {
			this.work = work;
			this.workListener = workListener;
			this.acceptOnExecution = acceptOnExecution;
		}

		@Override
		public void run() {
			if (this.acceptOnExecution) {
				this.workListener.workAccepted(new WorkEvent(this, WorkEvent.WORK_ACCEPTED, work, null));
			}
			synchronized (this.monitor) {
				this.started = true;
				this.monitor.notify();
			}
			this.workListener.workStarted(new WorkEvent(this, WorkEvent.WORK_STARTED, this.work, null));
			try {
				this.work.run();
			}
			catch (RuntimeException ex) {
				this.workListener.workCompleted(
						new WorkEvent(this, WorkEvent.WORK_COMPLETED, this.work, new WorkCompletedException(ex)));
				throw ex;
			}
			catch (Error err) {
				this.workListener.workCompleted(
						new WorkEvent(this, WorkEvent.WORK_COMPLETED, this.work, new WorkCompletedException(err)));
				throw err;
			}
			this.workListener.workCompleted(new WorkEvent(this, WorkEvent.WORK_COMPLETED, this.work, null));
		}

		@Override
		public void release() {
			this.work.release();
		}
	}

}
