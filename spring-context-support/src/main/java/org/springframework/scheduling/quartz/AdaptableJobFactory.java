package org.springframework.scheduling.quartz;

import org.quartz.Job;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;

/**
 * 支持{@link java.lang.Runnable}对象以及标准Quartz {@link org.quartz.Job}实例的JobFactory实现.
 *
 * <p>从Spring 4.1开始, 与Quartz 2.1.4及更高版本兼容.
 */
public class AdaptableJobFactory implements JobFactory {

	@Override
	public Job newJob(TriggerFiredBundle bundle, Scheduler scheduler) throws SchedulerException {
		try {
			Object jobObject = createJobInstance(bundle);
			return adaptJob(jobObject);
		}
		catch (Exception ex) {
			throw new SchedulerException("Job instantiation failed", ex);
		}
	}

	/**
	 * 创建指定作业类的实例.
	 * <p>可以重写以后处理作业实例.
	 * 
	 * @param bundle 从中获取JobDetail和与触发器触发有关的其他信息的TriggerFiredBundle
	 * 
	 * @return 作业实例
	 * @throws Exception 如果作业实例化失败
	 */
	protected Object createJobInstance(TriggerFiredBundle bundle) throws Exception {
		return bundle.getJobDetail().getJobClass().newInstance();
	}

	/**
	 * 将给定的作业对象调整为Quartz Job接口.
	 * <p>默认实现支持直接Quartz Jobs以及Runnables, 它们包含在DelegatingJob中.
	 * 
	 * @param jobObject 指定作业类的原始实例
	 * 
	 * @return 适配后的Quartz Job实例
	 * @throws Exception 如果给定的作业无法适配
	 */
	protected Job adaptJob(Object jobObject) throws Exception {
		if (jobObject instanceof Job) {
			return (Job) jobObject;
		}
		else if (jobObject instanceof Runnable) {
			return new DelegatingJob((Runnable) jobObject);
		}
		else {
			throw new IllegalArgumentException("Unable to execute job class [" + jobObject.getClass().getName() +
					"]: only [org.quartz.Job] and [java.lang.Runnable] supported.");
		}
	}

}
