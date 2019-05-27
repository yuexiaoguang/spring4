package org.springframework.scheduling;

import java.util.Date;

/**
 * 触发器对象的公共接口, 用于确定与之关联的任务的下一个执行时间.
 */
public interface Trigger {

	/**
	 * 根据给定的触发器上下文确定下一个执行时间.
	 * 
	 * @param triggerContext 上下文对象, 封装上次执行时间和上次完成时间
	 * 
	 * @return 触发器定义的下一个执行时间; 如果触发器不再触发, 则为{@code null}
	 */
	Date nextExecutionTime(TriggerContext triggerContext);

}
