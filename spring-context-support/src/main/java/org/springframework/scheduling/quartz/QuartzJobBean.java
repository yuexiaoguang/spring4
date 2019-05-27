package org.springframework.scheduling.quartz;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyAccessorFactory;

/**
 * Quartz Job接口的简单实现, 将传入的JobDataMap和SchedulerContext应用为bean属性值.
 * 这是合适的, 因为将为每次执行创建一个新的Job实例.
 * JobDataMap条目将使用相同的键覆盖SchedulerContext条目.
 *
 * <p>例如, 假设JobDataMap包含一个值为"5"的键"myParam":
 * 然后, Job实现可以公开int类型的bean属性"myParam"来接收这样的值, i.e. 一个方法"setMyParam(int)".
 * 这也适用于复杂类型, 如业务对象等.
 *
 * <p><b>请注意, 将依赖注入应用于Job实例的首选方法是通过JobFactory:</b>
 * 也就是说, 将{@link SpringBeanJobFactory}指定为Quartz JobFactory
 * (通常通过{@link SchedulerFactoryBean#setJobFactory} SchedulerFactoryBean的 "jobFactory"属性}).
 * 这允许实现依赖注入的Quartz作业, 而不依赖于Spring基类.
 */
public abstract class QuartzJobBean implements Job {

	/**
	 * 此实现将传入的作业数据映射应用为bean属性值, 然后委托给{@code executeInternal}.
	 */
	@Override
	public final void execute(JobExecutionContext context) throws JobExecutionException {
		try {
			BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(this);
			MutablePropertyValues pvs = new MutablePropertyValues();
			pvs.addPropertyValues(context.getScheduler().getContext());
			pvs.addPropertyValues(context.getMergedJobDataMap());
			bw.setPropertyValues(pvs, true);
		}
		catch (SchedulerException ex) {
			throw new JobExecutionException(ex);
		}
		executeInternal(context);
	}

	/**
	 * 执行实际的作业.
	 * 作业数据映射已经通过execute应用为bean属性值.
	 * 约定与标准Quartz execute方法完全相同.
	 */
	protected abstract void executeInternal(JobExecutionContext context) throws JobExecutionException;

}
