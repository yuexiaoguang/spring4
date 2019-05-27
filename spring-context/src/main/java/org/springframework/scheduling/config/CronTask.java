package org.springframework.scheduling.config;

import org.springframework.scheduling.support.CronTrigger;

/**
 * {@link TriggerTask}实现,
 * 根据{@linkplain org.springframework.scheduling.support.CronSequenceGenerator 标准cron表达式}定义要执行的{@code Runnable}.
 */
public class CronTask extends TriggerTask {

	private final String expression;


	/**
	 * @param runnable 要执行的底层任务
	 * @param expression 定义何时执行任务的cron表达式
	 */
	public CronTask(Runnable runnable, String expression) {
		this(runnable, new CronTrigger(expression));
	}

	/**
	 * @param runnable 要执行的底层任务
	 * @param cronTrigger 定义何时执行任务的cron触发器
	 */
	public CronTask(Runnable runnable, CronTrigger cronTrigger) {
		super(runnable, cronTrigger);
		this.expression = cronTrigger.getExpression();
	}


	public String getExpression() {
		return this.expression;
	}

}
