package org.springframework.scheduling;

import java.util.Date;

/**
 * 上下文对象, 封装给定任务的上次执行时间和上次完成时间.
 */
public interface TriggerContext {

	/**
	 * 返回任务的上次<i>调度</i>执行时间; 如果之前未调度, 则返回{@code null}.
	 */
	Date lastScheduledExecutionTime();

	/**
	 * 返回任务的上次<i>实际</i>执行时间; 如果之前没有调度, 则返回{@code null}.
	 */
	Date lastActualExecutionTime();

	/**
	 * 返回任务的上次完成时间; 如果之前没有调度, 则返回{@code null}.
	 */
	Date lastCompletionTime();

}
