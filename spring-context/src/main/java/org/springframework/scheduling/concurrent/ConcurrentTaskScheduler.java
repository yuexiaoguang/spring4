package org.springframework.scheduling.concurrent;

import java.util.Date;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.enterprise.concurrent.LastExecution;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;

import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.SimpleTriggerContext;
import org.springframework.scheduling.support.TaskUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ErrorHandler;

/**
 * 获取{@code java.util.concurrent.ScheduledExecutorService}
 * 并为其公开Spring {@link org.springframework.scheduling.TaskScheduler}的适配器.
 * 扩展{@link ConcurrentTaskExecutor}以实现{@link org.springframework.scheduling.SchedulingTaskExecutor}接口.
 *
 * <p>自动检测JSR-236 {@link javax.enterprise.concurrent.ManagedScheduledExecutorService},
 * 以便尽可能将其用于基于触发器的调度, 而不是Spring的本地触发器管理,
 * 最终委托给针对{@code java.util.concurrent.ScheduledExecutorService} API的基于延迟的常规调度.
 * 对于Java EE 7环境中的JSR-236样式查找, 请考虑使用{@link DefaultManagedTaskScheduler}.
 *
 * <p>请注意, 有一个预构建的{@link ThreadPoolTaskScheduler},
 * 允许在bean样式中定义{@link java.util.concurrent.ScheduledThreadPoolExecutor},
 * 直接将它暴露为Spring {@link org.springframework.scheduling.TaskScheduler}.
 * 这是原始ScheduledThreadPoolExecutor定义的一个方便的替代方法, 它具有对当前适配器类的单独定义.
 */
public class ConcurrentTaskScheduler extends ConcurrentTaskExecutor implements TaskScheduler {

	private static Class<?> managedScheduledExecutorServiceClass;

	static {
		try {
			managedScheduledExecutorServiceClass = ClassUtils.forName(
					"javax.enterprise.concurrent.ManagedScheduledExecutorService",
					ConcurrentTaskScheduler.class.getClassLoader());
		}
		catch (ClassNotFoundException ex) {
			// JSR-236 API not available...
			managedScheduledExecutorServiceClass = null;
		}
	}

	private ScheduledExecutorService scheduledExecutor;

	private boolean enterpriseConcurrentScheduler = false;

	private ErrorHandler errorHandler;


	/**
	 * 使用单个线程执行器作为默认值.
	 */
	public ConcurrentTaskScheduler() {
		super();
		setScheduledExecutor(null);
	}

	/**
	 * 使用给定的{@link java.util.concurrent.ScheduledExecutorService}作为共享委托.
	 * <p>自动检测JSR-236 {@link javax.enterprise.concurrent.ManagedScheduledExecutorService},
	 * 以便在可能的情况下将其用于基于触发器的调度, 而不是Spring的本地触发器管理.
	 * 
	 * @param scheduledExecutor 要委托给的{@link java.util.concurrent.ScheduledExecutorService},
	 * 用于{@link org.springframework.scheduling.SchedulingTaskExecutor}和{@link TaskScheduler}调用
	 */
	public ConcurrentTaskScheduler(ScheduledExecutorService scheduledExecutor) {
		super(scheduledExecutor);
		setScheduledExecutor(scheduledExecutor);
	}

	/**
	 * 使用给定的{@link java.util.concurrent.Executor}和{@link java.util.concurrent.ScheduledExecutorService}作为委托.
	 * <p>自动检测JSR-236 {@link javax.enterprise.concurrent.ManagedScheduledExecutorService},
	 * 以便在可能的情况下将其用于基于触发器的调度, 而不是Spring的本地触发器管理.
	 * 
	 * @param concurrentExecutor 要委托给的{@link java.util.concurrent.Executor},
	 * 用于{@link org.springframework.scheduling.SchedulingTaskExecutor}调用
	 * @param scheduledExecutor 要委托给的{@link java.util.concurrent.ScheduledExecutorService},
	 * 用于{@link TaskScheduler}调用
	 */
	public ConcurrentTaskScheduler(Executor concurrentExecutor, ScheduledExecutorService scheduledExecutor) {
		super(concurrentExecutor);
		setScheduledExecutor(scheduledExecutor);
	}


	/**
	 * 指定要委托给的{@link java.util.concurrent.ScheduledExecutorService}.
	 * <p>自动检测JSR-236 {@link javax.enterprise.concurrent.ManagedScheduledExecutorService},
	 * 以便在可能的情况下将其用于基于触发器的调度, 而不是Spring的本地触发器管理.
	 * <p>Note: 这仅适用于{@link TaskScheduler}调用.
	 * 如果希望给定的执行器也适用于{@link org.springframework.scheduling.SchedulingTaskExecutor}调用,
	 * 请将相同的执行器引用传递给{@link #setConcurrentExecutor}.
	 */
	public final void setScheduledExecutor(ScheduledExecutorService scheduledExecutor) {
		if (scheduledExecutor != null) {
			this.scheduledExecutor = scheduledExecutor;
			this.enterpriseConcurrentScheduler = (managedScheduledExecutorServiceClass != null &&
					managedScheduledExecutorServiceClass.isInstance(scheduledExecutor));
		}
		else {
			this.scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
			this.enterpriseConcurrentScheduler = false;
		}
	}

	/**
	 * 提供{@link ErrorHandler}策略.
	 */
	public void setErrorHandler(ErrorHandler errorHandler) {
		Assert.notNull(errorHandler, "ErrorHandler must not be null");
		this.errorHandler = errorHandler;
	}


	@Override
	public ScheduledFuture<?> schedule(Runnable task, Trigger trigger) {
		try {
			if (this.enterpriseConcurrentScheduler) {
				return new EnterpriseConcurrentTriggerScheduler().schedule(decorateTask(task, true), trigger);
			}
			else {
				ErrorHandler errorHandler =
						(this.errorHandler != null ? this.errorHandler : TaskUtils.getDefaultErrorHandler(true));
				return new ReschedulingRunnable(task, trigger, this.scheduledExecutor, errorHandler).schedule();
			}
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + this.scheduledExecutor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public ScheduledFuture<?> schedule(Runnable task, Date startTime) {
		long initialDelay = startTime.getTime() - System.currentTimeMillis();
		try {
			return this.scheduledExecutor.schedule(decorateTask(task, false), initialDelay, TimeUnit.MILLISECONDS);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + this.scheduledExecutor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Date startTime, long period) {
		long initialDelay = startTime.getTime() - System.currentTimeMillis();
		try {
			return this.scheduledExecutor.scheduleAtFixedRate(decorateTask(task, true), initialDelay, period, TimeUnit.MILLISECONDS);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + this.scheduledExecutor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long period) {
		try {
			return this.scheduledExecutor.scheduleAtFixedRate(decorateTask(task, true), 0, period, TimeUnit.MILLISECONDS);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + this.scheduledExecutor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Date startTime, long delay) {
		long initialDelay = startTime.getTime() - System.currentTimeMillis();
		try {
			return this.scheduledExecutor.scheduleWithFixedDelay(decorateTask(task, true), initialDelay, delay, TimeUnit.MILLISECONDS);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + this.scheduledExecutor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long delay) {
		try {
			return this.scheduledExecutor.scheduleWithFixedDelay(decorateTask(task, true), 0, delay, TimeUnit.MILLISECONDS);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + this.scheduledExecutor + "] did not accept task: " + task, ex);
		}
	}

	private Runnable decorateTask(Runnable task, boolean isRepeatingTask) {
		Runnable result = TaskUtils.decorateTaskWithErrorHandler(task, this.errorHandler, isRepeatingTask);
		if (this.enterpriseConcurrentScheduler) {
			result = ManagedTaskBuilder.buildManagedTask(result, task.toString());
		}
		return result;
	}


	/**
	 * 委托, 将Spring触发器适配为JSR-236触发器.
	 * 分隔成一个内部类, 以避免对JSR-236 API的硬依赖.
	 */
	private class EnterpriseConcurrentTriggerScheduler {

		public ScheduledFuture<?> schedule(Runnable task, final Trigger trigger) {
			ManagedScheduledExecutorService executor = (ManagedScheduledExecutorService) scheduledExecutor;
			return executor.schedule(task, new javax.enterprise.concurrent.Trigger() {
				@Override
				public Date getNextRunTime(LastExecution le, Date taskScheduledTime) {
					return (trigger.nextExecutionTime(le != null ?
							new SimpleTriggerContext(le.getScheduledStart(), le.getRunStart(), le.getRunEnd()) :
							new SimpleTriggerContext()));
				}
				@Override
				public boolean skipRun(LastExecution lastExecution, Date scheduledRunTime) {
					return false;
				}
			});
		}
	}

}
