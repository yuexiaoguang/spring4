package org.springframework.scheduling.config;

import org.springframework.beans.factory.SmartInitializingSingleton;

/**
 * {@link ScheduledTaskRegistrar}子类, 它将任务的实际调度重定向到{@link #afterSingletonsInstantiated()}回调 (从4.1.2开始).
 */
public class ContextLifecycleScheduledTaskRegistrar extends ScheduledTaskRegistrar implements SmartInitializingSingleton {

	@Override
	public void afterPropertiesSet() {
		// no-op
	}

	@Override
	public void afterSingletonsInstantiated() {
		scheduleTasks();
	}

}
