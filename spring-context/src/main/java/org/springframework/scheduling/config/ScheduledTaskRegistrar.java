package org.springframework.scheduling.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * 使用{@link TaskScheduler}注册任务的Helper bean, 通常使用cron表达式.
 *
 * <p>从Spring 3.1开始, {@code ScheduledTaskRegistrar}具有更突出的面向用户的角色,
 * 与@{@link org.springframework.scheduling.annotation.EnableAsync EnableAsync}注解
 * 及其{@link org.springframework.scheduling.annotation.SchedulingConfigurer SchedulingConfigurer}回调接口一起使用时.
 */
public class ScheduledTaskRegistrar implements InitializingBean, DisposableBean {

	private TaskScheduler taskScheduler;

	private ScheduledExecutorService localExecutor;

	private List<TriggerTask> triggerTasks;

	private List<CronTask> cronTasks;

	private List<IntervalTask> fixedRateTasks;

	private List<IntervalTask> fixedDelayTasks;

	private final Map<Task, ScheduledTask> unresolvedTasks = new HashMap<Task, ScheduledTask>(16);

	private final Set<ScheduledTask> scheduledTasks = new LinkedHashSet<ScheduledTask>(16);


	/**
	 * 设置注册计划任务的{@link TaskScheduler}.
	 */
	public void setTaskScheduler(TaskScheduler taskScheduler) {
		Assert.notNull(taskScheduler, "TaskScheduler must not be null");
		this.taskScheduler = taskScheduler;
	}

	/**
	 * 设置注册计划任务的{@link TaskScheduler}, 或将{@link java.util.concurrent.ScheduledExecutorService}包装为{@code TaskScheduler}.
	 */
	public void setScheduler(Object scheduler) {
		Assert.notNull(scheduler, "Scheduler object must not be null");
		if (scheduler instanceof TaskScheduler) {
			this.taskScheduler = (TaskScheduler) scheduler;
		}
		else if (scheduler instanceof ScheduledExecutorService) {
			this.taskScheduler = new ConcurrentTaskScheduler(((ScheduledExecutorService) scheduler));
		}
		else {
			throw new IllegalArgumentException("Unsupported scheduler type: " + scheduler.getClass());
		}
	}

	/**
	 * 返回此注册商的{@link TaskScheduler}实例 (may be {@code null}).
	 */
	public TaskScheduler getScheduler() {
		return this.taskScheduler;
	}


	/**
	 * 指定触发的任务.
	 */
	public void setTriggerTasks(Map<Runnable, Trigger> triggerTasks) {
		this.triggerTasks = new ArrayList<TriggerTask>();
		for (Map.Entry<Runnable, Trigger> task : triggerTasks.entrySet()) {
			addTriggerTask(new TriggerTask(task.getKey(), task.getValue()));
		}
	}

	/**
	 * 指定触发的任务.
	 * 主要由{@code <task:*>}命名空间解析使用.
	 */
	public void setTriggerTasksList(List<TriggerTask> triggerTasks) {
		this.triggerTasks = triggerTasks;
	}

	/**
	 * 获取触发器任务.
	 * 
	 * @return 不可修改的任务 (never {@code null})
	 */
	public List<TriggerTask> getTriggerTaskList() {
		return (this.triggerTasks != null? Collections.unmodifiableList(this.triggerTasks) :
				Collections.<TriggerTask>emptyList());
	}

	/**
	 * 指定触发的任务, Runnables (任务)对应cron 表达式.
	 */
	public void setCronTasks(Map<Runnable, String> cronTasks) {
		this.cronTasks = new ArrayList<CronTask>();
		for (Map.Entry<Runnable, String> task : cronTasks.entrySet()) {
			addCronTask(task.getKey(), task.getValue());
		}
	}

	/**
	 * 指定触发的任务.
	 * 主要由{@code <task:*>}命名空间解析使用.
	 */
	public void setCronTasksList(List<CronTask> cronTasks) {
		this.cronTasks = cronTasks;
	}

	/**
	 * 获取cron任务.
	 * 
	 * @return 不可修改的任务列表 (never {@code null})
	 */
	public List<CronTask> getCronTaskList() {
		return (this.cronTasks != null ? Collections.unmodifiableList(this.cronTasks) :
				Collections.<CronTask>emptyList());
	}

	/**
	 * 指定触发的任务, Runnables (任务)对应固定速率值.
	 */
	public void setFixedRateTasks(Map<Runnable, Long> fixedRateTasks) {
		this.fixedRateTasks = new ArrayList<IntervalTask>();
		for (Map.Entry<Runnable, Long> task : fixedRateTasks.entrySet()) {
			addFixedRateTask(task.getKey(), task.getValue());
		}
	}

	/**
	 * 指定固定速率任务.
	 * 主要由{@code <task:*>}命名空间解析使用.
	 */
	public void setFixedRateTasksList(List<IntervalTask> fixedRateTasks) {
		this.fixedRateTasks = fixedRateTasks;
	}

	/**
	 * 获取固定速率任务.
	 * 
	 * @return 不可修改的任务列表 (never {@code null})
	 */
	public List<IntervalTask> getFixedRateTaskList() {
		return (this.fixedRateTasks != null ? Collections.unmodifiableList(this.fixedRateTasks) :
				Collections.<IntervalTask>emptyList());
	}

	/**
	 * 指定触发的任务, Runnables (任务)对应固定延迟值.
	 */
	public void setFixedDelayTasks(Map<Runnable, Long> fixedDelayTasks) {
		this.fixedDelayTasks = new ArrayList<IntervalTask>();
		for (Map.Entry<Runnable, Long> task : fixedDelayTasks.entrySet()) {
			addFixedDelayTask(task.getKey(), task.getValue());
		}
	}

	/**
	 * 指定固定延迟任务.
	 * 主要由{@code <task:*>}命名空间解析使用.
	 */
	public void setFixedDelayTasksList(List<IntervalTask> fixedDelayTasks) {
		this.fixedDelayTasks = fixedDelayTasks;
	}

	/**
	 * 获取固定延迟任务..
	 * 
	 * @return 不可修改的任务列表 (never {@code null})
	 */
	public List<IntervalTask> getFixedDelayTaskList() {
		return (this.fixedDelayTasks != null ? Collections.unmodifiableList(this.fixedDelayTasks) :
				Collections.<IntervalTask>emptyList());
	}


	/**
	 * 根据给定的{@link Trigger}添加要触发的Runnable任务.
	 */
	public void addTriggerTask(Runnable task, Trigger trigger) {
		addTriggerTask(new TriggerTask(task, trigger));
	}

	/**
	 * 添加{@code TriggerTask}.
	 */
	public void addTriggerTask(TriggerTask task) {
		if (this.triggerTasks == null) {
			this.triggerTasks = new ArrayList<TriggerTask>();
		}
		this.triggerTasks.add(task);
	}

	/**
	 * 根据给定的cron表达式添加要触发的Runnable任务
	 */
	public void addCronTask(Runnable task, String expression) {
		addCronTask(new CronTask(task, expression));
	}

	/**
	 * 添加{@link CronTask}.
	 */
	public void addCronTask(CronTask task) {
		if (this.cronTasks == null) {
			this.cronTasks = new ArrayList<CronTask>();
		}
		this.cronTasks.add(task);
	}

	/**
	 * 添加以给定固定速率间隔触发的{@code Runnable}任务.
	 */
	public void addFixedRateTask(Runnable task, long interval) {
		addFixedRateTask(new IntervalTask(task, interval, 0));
	}

	/**
	 * 添加固定速率{@link IntervalTask​​}.
	 */
	public void addFixedRateTask(IntervalTask task) {
		if (this.fixedRateTasks == null) {
			this.fixedRateTasks = new ArrayList<IntervalTask>();
		}
		this.fixedRateTasks.add(task);
	}

	/**
	 * 添加要使用给定的固定延迟触发的Runnable任务.
	 */
	public void addFixedDelayTask(Runnable task, long delay) {
		addFixedDelayTask(new IntervalTask(task, delay, 0));
	}

	/**
	 * 添加固定延迟{@link IntervalTask​​}.
	 */
	public void addFixedDelayTask(IntervalTask task) {
		if (this.fixedDelayTasks == null) {
			this.fixedDelayTasks = new ArrayList<IntervalTask>();
		}
		this.fixedDelayTasks.add(task);
	}


	/**
	 * 返回此{@code ScheduledTaskRegistrar}是否有已注册的任务.
	 */
	public boolean hasTasks() {
		return (!CollectionUtils.isEmpty(this.triggerTasks) ||
				!CollectionUtils.isEmpty(this.cronTasks) ||
				!CollectionUtils.isEmpty(this.fixedRateTasks) ||
				!CollectionUtils.isEmpty(this.fixedDelayTasks));
	}


	/**
	 * 在bean构建时调用{@link #scheduleTasks()}.
	 */
	@Override
	public void afterPropertiesSet() {
		scheduleTasks();
	}

	/**
	 * 根据底层{@linkplain #setTaskScheduler(TaskScheduler) 任务调度器}调度所有已注册的任务.
	 */
	protected void scheduleTasks() {
		if (this.taskScheduler == null) {
			this.localExecutor = Executors.newSingleThreadScheduledExecutor();
			this.taskScheduler = new ConcurrentTaskScheduler(this.localExecutor);
		}
		if (this.triggerTasks != null) {
			for (TriggerTask task : this.triggerTasks) {
				addScheduledTask(scheduleTriggerTask(task));
			}
		}
		if (this.cronTasks != null) {
			for (CronTask task : this.cronTasks) {
				addScheduledTask(scheduleCronTask(task));
			}
		}
		if (this.fixedRateTasks != null) {
			for (IntervalTask task : this.fixedRateTasks) {
				addScheduledTask(scheduleFixedRateTask(task));
			}
		}
		if (this.fixedDelayTasks != null) {
			for (IntervalTask task : this.fixedDelayTasks) {
				addScheduledTask(scheduleFixedDelayTask(task));
			}
		}
	}

	private void addScheduledTask(ScheduledTask task) {
		if (task != null) {
			this.scheduledTasks.add(task);
		}
	}


	/**
	 * 在调度器初始化时, 或立即调度指定的触发器任务.
	 * 
	 * @return 计划任务的句柄, 允许取消它
	 */
	public ScheduledTask scheduleTriggerTask(TriggerTask task) {
		ScheduledTask scheduledTask = this.unresolvedTasks.remove(task);
		boolean newTask = false;
		if (scheduledTask == null) {
			scheduledTask = new ScheduledTask();
			newTask = true;
		}
		if (this.taskScheduler != null) {
			scheduledTask.future = this.taskScheduler.schedule(task.getRunnable(), task.getTrigger());
		}
		else {
			addTriggerTask(task);
			this.unresolvedTasks.put(task, scheduledTask);
		}
		return (newTask ? scheduledTask : null);
	}

	/**
	 * 在调度器初始化时, 或立即调度指定的cron任务.
	 * 
	 * @return 计划任务的句柄, 允许取消它
	 * (或{@code null}如果处理先前注册的任务)
	 */
	public ScheduledTask scheduleCronTask(CronTask task) {
		ScheduledTask scheduledTask = this.unresolvedTasks.remove(task);
		boolean newTask = false;
		if (scheduledTask == null) {
			scheduledTask = new ScheduledTask();
			newTask = true;
		}
		if (this.taskScheduler != null) {
			scheduledTask.future = this.taskScheduler.schedule(task.getRunnable(), task.getTrigger());
		}
		else {
			addCronTask(task);
			this.unresolvedTasks.put(task, scheduledTask);
		}
		return (newTask ? scheduledTask : null);
	}

	/**
	 * 在调度器初始化时, 或立即调度指定的固定速率任务.
	 * 
	 * @return 计划任务的句柄, 允许取消它
	 * (或{@code null}如果处理先前注册的任务)
	 */
	public ScheduledTask scheduleFixedRateTask(IntervalTask task) {
		ScheduledTask scheduledTask = this.unresolvedTasks.remove(task);
		boolean newTask = false;
		if (scheduledTask == null) {
			scheduledTask = new ScheduledTask();
			newTask = true;
		}
		if (this.taskScheduler != null) {
			if (task.getInitialDelay() > 0) {
				Date startTime = new Date(System.currentTimeMillis() + task.getInitialDelay());
				scheduledTask.future =
						this.taskScheduler.scheduleAtFixedRate(task.getRunnable(), startTime, task.getInterval());
			}
			else {
				scheduledTask.future =
						this.taskScheduler.scheduleAtFixedRate(task.getRunnable(), task.getInterval());
			}
		}
		else {
			addFixedRateTask(task);
			this.unresolvedTasks.put(task, scheduledTask);
		}
		return (newTask ? scheduledTask : null);
	}

	/**
	 * 在调度器初始化时, 或立即调度指定的固定延迟任务.
	 * 
	 * @return 计划任务的句柄, 允许取消它
	 * (或{@code null}如果处理先前注册的任务)
	 */
	public ScheduledTask scheduleFixedDelayTask(IntervalTask task) {
		ScheduledTask scheduledTask = this.unresolvedTasks.remove(task);
		boolean newTask = false;
		if (scheduledTask == null) {
			scheduledTask = new ScheduledTask();
			newTask = true;
		}
		if (this.taskScheduler != null) {
			if (task.getInitialDelay() > 0) {
				Date startTime = new Date(System.currentTimeMillis() + task.getInitialDelay());
				scheduledTask.future =
						this.taskScheduler.scheduleWithFixedDelay(task.getRunnable(), startTime, task.getInterval());
			}
			else {
				scheduledTask.future =
						this.taskScheduler.scheduleWithFixedDelay(task.getRunnable(), task.getInterval());
			}
		}
		else {
			addFixedDelayTask(task);
			this.unresolvedTasks.put(task, scheduledTask);
		}
		return (newTask ? scheduledTask : null);
	}


	@Override
	public void destroy() {
		for (ScheduledTask task : this.scheduledTasks) {
			task.cancel();
		}
		if (this.localExecutor != null) {
			this.localExecutor.shutdownNow();
		}
	}

}
