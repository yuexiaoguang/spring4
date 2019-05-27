package org.springframework.scheduling.config;

/**
 * 持有者类, 定义要作为任务执行的{@code Runnable}, 通常是在计划的时间或间隔.
 * 有关各种调度方法, 请参阅子类层次结构.
 */
public class Task {

	private final Runnable runnable;


	/**
	 * @param runnable 要执行的底层任务.
	 */
	public Task(Runnable runnable) {
		this.runnable = runnable;
	}


	public Runnable getRunnable() {
		return runnable;
	}
}
