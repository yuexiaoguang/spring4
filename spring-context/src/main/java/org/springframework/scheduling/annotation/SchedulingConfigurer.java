package org.springframework.scheduling.annotation;

import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * 由带@{@link EnableScheduling}注解的
 * @{@link org.springframework.context.annotation.Configuration Configuration}类实现的可选接口.
 * 通常用于设置要使用的特定{@link org.springframework.scheduling.TaskScheduler TaskScheduler} bean,
 * 在执行定时任务或以<em>编程</em>方式注册定时任务, 而不是使用@{@link Scheduled} 注解的<em>声明性</em>方法时.
 * 例如, 在实现基于{@link org.springframework.scheduling.Trigger Trigger}的任务时,
 * {@code @Scheduled}注解不支持这些任务, 可能是必要的.
 *
 * <p>See @{@link EnableScheduling} for detailed usage examples.
 */
public interface SchedulingConfigurer {

	/**
	 * 回调, 允许{@link org.springframework.scheduling.TaskScheduler TaskScheduler}
	 * 和特定的{@link org.springframework.scheduling.config.Task Task}实例针对给定的{@link ScheduledTaskRegistrar}进行注册
	 * 
	 * @param taskRegistrar 要配置的注册商.
	 */
	void configureTasks(ScheduledTaskRegistrar taskRegistrar);

}
