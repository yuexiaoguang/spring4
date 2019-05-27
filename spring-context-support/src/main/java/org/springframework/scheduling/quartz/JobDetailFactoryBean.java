package org.springframework.scheduling.quartz;

import java.util.Map;

import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.impl.JobDetailImpl;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Spring {@link FactoryBean}用于创建Quartz {@link org.quartz.JobDetail}实例, 支持JobDetail配置的bean风格用法.
 *
 * <p>{@code JobDetail(Impl)}本身已经是一个JavaBean, 但缺乏合理的默认值.
 * 此类使用Spring bean名称作为作业名称, 如果未指定, 则使用Quartz默认组("DEFAULT")作为作业组.
 */
public class JobDetailFactoryBean
		implements FactoryBean<JobDetail>, BeanNameAware, ApplicationContextAware, InitializingBean {

	private String name;

	private String group;

	private Class<?> jobClass;

	private JobDataMap jobDataMap = new JobDataMap();

	private boolean durability = false;

	private boolean requestsRecovery = false;

	private String description;

	private String beanName;

	private ApplicationContext applicationContext;

	private String applicationContextJobDataKey;

	private JobDetail jobDetail;


	/**
	 * 指定作业的名称.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * 指定作业的组.
	 */
	public void setGroup(String group) {
		this.group = group;
	}

	/**
	 * 指定作业的实现类.
	 */
	public void setJobClass(Class<?> jobClass) {
		this.jobClass = jobClass;
	}

	/**
	 * 指定作业的JobDataMap.
	 */
	public void setJobDataMap(JobDataMap jobDataMap) {
		this.jobDataMap = jobDataMap;
	}

	/**
	 * 返回作业的JobDataMap.
	 */
	public JobDataMap getJobDataMap() {
		return this.jobDataMap;
	}

	/**
	 * 通过给定的Map在JobDataMap中注册对象.
	 * <p>与SchedulerContext中的对象相比, 这些对象仅可用于此作业.
	 * <p>Note: 使用其JobDetail保存在数据库中的持久作业时,
	 * 不要将Spring管理的bean或ApplicationContext引用放入JobDataMap, 而是放入SchedulerContext.
	 * 
	 * @param jobDataAsMap 对象作为值(例如Spring管理的bean)
	 */
	public void setJobDataAsMap(Map<String, ?> jobDataAsMap) {
		getJobDataMap().putAll(jobDataAsMap);
	}

	/**
	 * 指定作业的持久性, i.e. 是否应该保留存储在作业存储中, 即使没有触发器指向它.
	 */
	public void setDurability(boolean durability) {
		this.durability = durability;
	}

	/**
	 * 设置此作业的恢复标志, i.e. 如果遇到'恢复'或'故障转移'情况, 是否应重新执行作业.
	 */
	public void setRequestsRecovery(boolean requestsRecovery) {
		this.requestsRecovery = requestsRecovery;
	}

	/**
	 * 设置此作业的文本说明.
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	/**
	 * 设置ApplicationContext引用的键以在JobDataMap中公开, 例如"applicationContext". 默认无.
	 * 仅适用于在Spring ApplicationContext中运行时.
	 * <p>对于QuartzJobBean, 引用将作为bean属性应用于Job实例.
	 * "applicationContext"属性将对应于该场景中的"setApplicationContext"方法.
	 * <p>请注意, 像ApplicationContextAware这样的BeanFactory回调接口不会自动应用于Quartz Job实例,
	 * 因为Quartz本身负责其作业的生命周期.
	 * <p><b>Note: 当使用其JobDetail内容将保存在数据库中的持久性作业存储时, 不要将ApplicationContext引用放入JobDataMap,
	 * 而是放入SchedulerContext.</b>
	 */
	public void setApplicationContextJobDataKey(String applicationContextJobDataKey) {
		this.applicationContextJobDataKey = applicationContextJobDataKey;
	}


	@Override
	@SuppressWarnings("unchecked")
	public void afterPropertiesSet() {
		if (this.name == null) {
			this.name = this.beanName;
		}
		if (this.group == null) {
			this.group = Scheduler.DEFAULT_GROUP;
		}
		if (this.applicationContextJobDataKey != null) {
			if (this.applicationContext == null) {
				throw new IllegalStateException(
					"JobDetailBean needs to be set up in an ApplicationContext " +
					"to be able to handle an 'applicationContextJobDataKey'");
			}
			getJobDataMap().put(this.applicationContextJobDataKey, this.applicationContext);
		}

		JobDetailImpl jdi = new JobDetailImpl();
		jdi.setName(this.name);
		jdi.setGroup(this.group);
		jdi.setJobClass((Class) this.jobClass);
		jdi.setJobDataMap(this.jobDataMap);
		jdi.setDurability(this.durability);
		jdi.setRequestsRecovery(this.requestsRecovery);
		jdi.setDescription(this.description);
		this.jobDetail = jdi;
	}


	@Override
	public JobDetail getObject() {
		return this.jobDetail;
	}

	@Override
	public Class<?> getObjectType() {
		return JobDetail.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
