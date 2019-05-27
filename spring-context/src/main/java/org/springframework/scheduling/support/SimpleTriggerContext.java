package org.springframework.scheduling.support;

import java.util.Date;

import org.springframework.scheduling.TriggerContext;

/**
 * {@link TriggerContext}接口的简单数据持有者实现.
 */
public class SimpleTriggerContext implements TriggerContext {

	private volatile Date lastScheduledExecutionTime;

	private volatile Date lastActualExecutionTime;

	private volatile Date lastCompletionTime;


	/**
	 * 所有时间值设置为{@code null}.
	 */
	 public SimpleTriggerContext() {
	}

	/**
	 * @param lastScheduledExecutionTime 上一次<i>计划</i>执行时间
	 * @param lastActualExecutionTime 上一次<i>实际</i>执行时间
	 * @param lastCompletionTime 上一次完成时间
	 */
	public SimpleTriggerContext(Date lastScheduledExecutionTime, Date lastActualExecutionTime, Date lastCompletionTime) {
		this.lastScheduledExecutionTime = lastScheduledExecutionTime;
		this.lastActualExecutionTime = lastActualExecutionTime;
		this.lastCompletionTime = lastCompletionTime;
	}


	/**
	 * 使用最新时间值更新此持有者的状态.
	 * 
 	 * @param lastScheduledExecutionTime 上一次<i>计划</i>执行时间
	 * @param lastActualExecutionTime 上一次<i>实际</i>执行时间
	 * @param lastCompletionTime 上一次完成时间
	 */
	public void update(Date lastScheduledExecutionTime, Date lastActualExecutionTime, Date lastCompletionTime) {
		this.lastScheduledExecutionTime = lastScheduledExecutionTime;
		this.lastActualExecutionTime = lastActualExecutionTime;
		this.lastCompletionTime = lastCompletionTime;
	}


	@Override
	public Date lastScheduledExecutionTime() {
		return this.lastScheduledExecutionTime;
	}

	@Override
	public Date lastActualExecutionTime() {
		return this.lastActualExecutionTime;
	}

	@Override
	public Date lastCompletionTime() {
		return this.lastCompletionTime;
	}

}
