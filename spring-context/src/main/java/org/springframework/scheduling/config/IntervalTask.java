package org.springframework.scheduling.config;

/**
 * {@link Task}实现, 定义以给定的毫秒间隔执行的{@code Runnable}, 根据上下文可以将其视为固定速率或固定延迟.
 */
public class IntervalTask extends Task {

	private final long interval;

	private final long initialDelay;


	/**
	 * @param runnable 要执行的底层任务
	 * @param interval 执行任务的频率, 以毫秒为单位
	 * @param initialDelay 首次执行任务之前的初始延迟
	 */
	public IntervalTask(Runnable runnable, long interval, long initialDelay) {
		super(runnable);
		this.interval = interval;
		this.initialDelay = initialDelay;
	}

	/**
	 * @param runnable 要执行的底层任务
	 * @param interval 执行任务的频率, 以毫秒为单位
	 */
	public IntervalTask(Runnable runnable, long interval) {
		this(runnable, interval, 0);
	}


	public long getInterval() {
		return this.interval;
	}

	public long getInitialDelay() {
		return this.initialDelay;
	}

}
