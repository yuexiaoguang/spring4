package org.springframework.scheduling.commonj;

import commonj.timers.TimerListener;

/**
 * 描述TimerListener的JavaBean, 它由TimerListener本身(或用于创建TimerListener的Runnable)和延迟加周期组成.
 * 需要指定周期; 默认情况下没有任何意义.
 *
 * <p>CommonJ TimerManager不提供更复杂的调度选项, 如cron表达式.
 * 考虑使用Quartz来满足这些高级需求.
 *
 * <p>请注意, TimerManager使用在重复执行之间共享的TimerListener实例, 而Quartz则为每次执行实例化一个新Job.
 */
public class ScheduledTimerListener {

	private TimerListener timerListener;

	private long delay = 0;

	private long period = -1;

	private boolean fixedRate = false;


	/**
	 * 通过bean属性填充.
	 */
	public ScheduledTimerListener() {
	}

	/**
	 * @param timerListener 要调度的TimerListener
	 */
	public ScheduledTimerListener(TimerListener timerListener) {
		this.timerListener = timerListener;
	}

	/**
	 * 具有给定延迟的默认一次性执行.
	 * 
	 * @param timerListener 要调度的TimerListener
	 * @param delay 第一次启动任务之前的延迟 (ms)
	 */
	public ScheduledTimerListener(TimerListener timerListener, long delay) {
		this.timerListener = timerListener;
		this.delay = delay;
	}

	/**
	 * @param timerListener 要调度的TimerListener
	 * @param delay 第一次启动任务之前的延迟 (ms)
	 * @param period 重复执行的任务之间的时间间隔 (ms)
	 * @param fixedRate 是否按固定速率执行
	 */
	public ScheduledTimerListener(TimerListener timerListener, long delay, long period, boolean fixedRate) {
		this.timerListener = timerListener;
		this.delay = delay;
		this.period = period;
		this.fixedRate = fixedRate;
	}

	/**
	 * 默认一次性执行没有延迟.
	 * 
	 * @param timerTask 要调度的Runnable
	 */
	public ScheduledTimerListener(Runnable timerTask) {
		setRunnable(timerTask);
	}

	/**
	 * 具有给定延迟的默认一次性执行.
	 * 
	 * @param timerTask 要调度的Runnable
	 * @param delay 第一次启动任务之前的延迟 (ms)
	 */
	public ScheduledTimerListener(Runnable timerTask, long delay) {
		setRunnable(timerTask);
		this.delay = delay;
	}

	/**
	 * @param timerTask 要调度的Runnable
	 * @param delay 第一次启动任务之前的延迟 (ms)
	 * @param period 重复执行的任务之间的时间间隔 (ms)
	 * @param fixedRate 是否按固定速率执行
	 */
	public ScheduledTimerListener(Runnable timerTask, long delay, long period, boolean fixedRate) {
		setRunnable(timerTask);
		this.delay = delay;
		this.period = period;
		this.fixedRate = fixedRate;
	}


	/**
	 * 设置要调度的Runnable.
	 */
	public void setRunnable(Runnable timerTask) {
		this.timerListener = new DelegatingTimerListener(timerTask);
	}

	/**
	 * 设置要调度的TimerListener.
	 */
	public void setTimerListener(TimerListener timerListener) {
		this.timerListener = timerListener;
	}

	/**
	 * 返回要调度的TimerListener.
	 */
	public TimerListener getTimerListener() {
		return this.timerListener;
	}

	/**
	 * 设置第一次启动任务之前的延迟, 以毫秒为单位.
	 * 默认 0, 成功调度后立即启动任务.
	 * <p>如果指定了"firstTime"属性, 则将忽略此属性.
	 * 指定一个或另一个, 而不是两者.
	 */
	public void setDelay(long delay) {
		this.delay = delay;
	}

	/**
	 * 返回第一次启动任务之前的延迟, 以毫秒为单位.
	 */
	public long getDelay() {
		return this.delay;
	}

	/**
	 * 设置要重复执行的任务之间的时间间隔, 以毫秒为单位.
	 * <p>默认 -1, 导致一次性执行.
	 * 在零值或正值的情况下, 任务将重复执行, 使用给定间隔.
	 * <p>注意, 周期值的语义在固定速率和固定延迟执行之间变化.
	 * <p><b>Note:</b> 支持0的周期 (例如, 作为固定延迟), 因为CommonJ规范将其定义为合法值.
	 * 因此, 值0将导致在作业完成后立即重新执行 (不像{@code java.util.Timer}那样一次性执行).
	 */
	public void setPeriod(long period) {
		this.period = period;
	}

	/**
	 * 返回重复执行任务之间的时间段.
	 */
	public long getPeriod() {
		return this.period;
	}

	/**
	 * 此任务是否只执行一次?
	 * 
	 * @return {@code true}如果这个任务只执行一次
	 */
	public boolean isOneTimeTask() {
		return (this.period < 0);
	}

	/**
	 * 设置是否按固定速率执行, 而不是固定延迟执行. 默认"false", i.e. 固定延迟.
	 * <p>有关这些执行模式的详细信息, 请参阅TimerManager javadoc.
	 */
	public void setFixedRate(boolean fixedRate) {
		this.fixedRate = fixedRate;
	}

	/**
	 * 返回是否按固定速率执行.
	 */
	public boolean isFixedRate() {
		return this.fixedRate;
	}
}
