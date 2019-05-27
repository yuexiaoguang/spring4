package org.springframework.scheduling.config;

import org.springframework.scheduling.Trigger;

/**
 * {@link Task}实现, 根据给定的{@link Trigger}定义要执行的{@code Runnable}.
 */
public class TriggerTask extends Task {

	private final Trigger trigger;


	/**
	 * @param runnable 要执行的底层任务
	 * @param trigger 指定何时执行任务
	 */
	public TriggerTask(Runnable runnable, Trigger trigger) {
		super(runnable);
		this.trigger = trigger;
	}


	public Trigger getTrigger() {
		return this.trigger;
	}
}
