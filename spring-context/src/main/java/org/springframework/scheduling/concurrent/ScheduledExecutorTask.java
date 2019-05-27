package org.springframework.scheduling.concurrent;

import java.util.concurrent.TimeUnit;

/**
 * 描述调度执行器任务的JavaBean, 由{@link Runnable}和延迟加上周期组成.
 * 需要指定的周期; 默认情况下没有任何意义.
 *
 * <p>{@link java.util.concurrent.ScheduledExecutorService}不提供更复杂的调度选项, 例如cron表达式.
 * 考虑使用{@link ThreadPoolTask​​Scheduler}来满足这些需求.
 *
 * <p>请注意, 在重复执行之间, {@link java.util.concurrent.ScheduledExecutorService}机制使用共享的{@link Runnable}实例,
 * 而Quartz为每次执行创建一个新的Job实例.
 */
public class ScheduledExecutorTask {

	private Runnable runnable;

	private long delay = 0;

	private long period = -1;

	private TimeUnit timeUnit = TimeUnit.MILLISECONDS;

	private boolean fixedRate = false;


	/**
	 * 通过bean属性填充.
	 */
	public ScheduledExecutorTask() {
	}

	/**
	 * 默认一次性执行没有延迟.
	 * 
	 * @param executorTask 要调度的Runnable
	 */
	public ScheduledExecutorTask(Runnable executorTask) {
		this.runnable = executorTask;
	}

	/**
	 * 具有给定延迟的默认一次性执行.
	 * 
	 * @param executorTask 要调度的Runnable
	 * @param delay 第一次启动任务之前的延迟 (ms)
	 */
	public ScheduledExecutorTask(Runnable executorTask, long delay) {
		this.runnable = executorTask;
		this.delay = delay;
	}

	/**
	 * @param executorTask 要调度的Runnable
	 * @param delay 第一次启动任务之前的延迟 (ms)
	 * @param period 重复执行任务之间的时间间隔 (ms)
	 * @param fixedRate 是否计划为固定速率执行
	 */
	public ScheduledExecutorTask(Runnable executorTask, long delay, long period, boolean fixedRate) {
		this.runnable = executorTask;
		this.delay = delay;
		this.period = period;
		this.fixedRate = fixedRate;
	}


	/**
	 * 将Runnable设置为计划执行任务.
	 */
	public void setRunnable(Runnable executorTask) {
		this.runnable = executorTask;
	}

	/**
	 * 返回作为计划执行任务的Runnable.
	 */
	public Runnable getRunnable() {
		return this.runnable;
	}

	/**
	 * 设置在第一次启动任务之前的延迟, 以毫秒为单位.
	 * 默认 0, 成功调度后立即启动任务.
	 */
	public void setDelay(long delay) {
		this.delay = delay;
	}

	/**
	 * 返回在第一次启动任务之前的延迟, 以毫秒为单位.
	 */
	public long getDelay() {
		return this.delay;
	}

	/**
	 * 设置重复的任务执行之间的时间间隔, 以毫秒为单位.
	 * <p>默认 -1, 导致一次性执行.
	 * 如果值为正值, 则任务将重复执行, 使用执行之间给定的间隔.
	 * <p>注意, 周期值的语义在固定速率和固定延迟执行之间变化.
	 * <p><b>Note:</b> 不支持0周期(例如固定延迟), 只是因为{@code java.util.concurrent.ScheduledExecutorService}本身不支持它.
	 * 因此, 值0将被视为一次性执行; 但是, 永远不应该首先明确指定该值!
	 */
	public void setPeriod(long period) {
		this.period = period;
	}

	/**
	 * 返回重复执行任务之间的周期.
	 */
	public long getPeriod() {
		return this.period;
	}

	/**
	 * 此任务是否只执行一次?
	 * 
	 * @return {@code true}如果此任务只执行一次
	 */
	public boolean isOneTimeTask() {
		return (this.period <= 0);
	}

	/**
	 * 指定延迟和周期值的时间单位.
	 * 默认 ({@code TimeUnit.MILLISECONDS}).
	 */
	public void setTimeUnit(TimeUnit timeUnit) {
		this.timeUnit = (timeUnit != null ? timeUnit : TimeUnit.MILLISECONDS);
	}

	/**
	 * 返回延迟和周期值的时间单位.
	 */
	public TimeUnit getTimeUnit() {
		return this.timeUnit;
	}

	/**
	 * 设置是否固定速率执行, 而不是固定延迟执行. 默认"false", 即, 固定延迟.
	 * <p>有关这些执行模式的详细信息, 请参阅ScheduledExecutorService javadoc.
	 */
	public void setFixedRate(boolean fixedRate) {
		this.fixedRate = fixedRate;
	}

	/**
	 * 返回是否固定速率执行.
	 */
	public boolean isFixedRate() {
		return this.fixedRate;
	}
}
