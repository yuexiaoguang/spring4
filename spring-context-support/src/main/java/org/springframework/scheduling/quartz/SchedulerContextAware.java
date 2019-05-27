package org.springframework.scheduling.quartz;

import org.quartz.SchedulerContext;

import org.springframework.beans.factory.Aware;

/**
 * 回调接口, 由Spring管理的Quartz工件实现, 需要访问SchedulerContext (没有自然访问它).
 *
 * <p>目前仅支持通过Spring的SchedulerFactoryBean传递的自定义JobFactory实现.
 */
public interface SchedulerContextAware extends Aware {

	/**
	 * 设置当前Quartz Scheduler的SchedulerContext.
	 */
	void setSchedulerContext(SchedulerContext schedulerContext);

}
