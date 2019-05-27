package org.springframework.scheduling.quartz;

import org.quartz.SchedulerContext;
import org.quartz.spi.TriggerFiredBundle;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyAccessorFactory;

/**
 * {@link AdaptableJobFactory}的子类, 它还支持对bean属性的Spring样式依赖注入.
 * 这基本上是Spring的{@link QuartzJobBean}的直接等价形式的Quartz {@link org.quartz.spi.JobFactory}.
 *
 * <p>将调度程序上下文, 作业数据映射和触发器数据映射条目应用为bean属性值.
 * 如果未找到匹配的bean属性, 则默认情况下将忽略该条目. 这类似于QuartzJobBean的行为.
 *
 * <p>从Spring 4.1开始, 与Quartz 2.1.4及更高版本兼容.
 */
public class SpringBeanJobFactory extends AdaptableJobFactory implements SchedulerContextAware {

	private String[] ignoredUnknownProperties;

	private SchedulerContext schedulerContext;


	/**
	 * 指定应忽略的未知属性 (在bean中找不到).
	 * <p>默认为{@code null}, 表示应忽略所有未知属性.
	 * 指定一个空数组, 以便在出现任何未知属性时抛出异常;
	 * 或者指定一个应忽略的属性名称列表, 如果在特定作业类上找不到相应属性 (所有其他未知属性仍将触发异常).
	 */
	public void setIgnoredUnknownProperties(String... ignoredUnknownProperties) {
		this.ignoredUnknownProperties = ignoredUnknownProperties;
	}

	@Override
	public void setSchedulerContext(SchedulerContext schedulerContext) {
		this.schedulerContext = schedulerContext;
	}


	/**
	 * 创建作业实例, 使用从调度程序上下文, 作业数据映射, 触发器数据映射中获取的属性值填充它.
	 */
	@Override
	protected Object createJobInstance(TriggerFiredBundle bundle) throws Exception {
		Object job = super.createJobInstance(bundle);
		if (isEligibleForPropertyPopulation(job)) {
			BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(job);
			MutablePropertyValues pvs = new MutablePropertyValues();
			if (this.schedulerContext != null) {
				pvs.addPropertyValues(this.schedulerContext);
			}
			pvs.addPropertyValues(bundle.getJobDetail().getJobDataMap());
			pvs.addPropertyValues(bundle.getTrigger().getJobDataMap());
			if (this.ignoredUnknownProperties != null) {
				for (String propName : this.ignoredUnknownProperties) {
					if (pvs.contains(propName) && !bw.isWritableProperty(propName)) {
						pvs.removePropertyValue(propName);
					}
				}
				bw.setPropertyValues(pvs);
			}
			else {
				bw.setPropertyValues(pvs, true);
			}
		}
		return job;
	}

	/**
	 * 返回给定作业对象是否有资格填充其bean属性.
	 * <p>默认实现忽略了{@link QuartzJobBean}实例, 它们将自己注入bean属性.
	 * 
	 * @param jobObject 要内省的作业对象
	 */
	protected boolean isEligibleForPropertyPopulation(Object jobObject) {
		return (!(jobObject instanceof QuartzJobBean));
	}

}
