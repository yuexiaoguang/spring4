package org.springframework.scheduling.quartz;

import java.util.Date;
import java.util.Map;

import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SimpleTrigger;
import org.quartz.impl.triggers.SimpleTriggerImpl;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.Constants;
import org.springframework.util.Assert;

/**
 * Spring {@link FactoryBean}, 用于创建Quartz {@link org.quartz.SimpleTrigger}实例, 支持用于触发器配置的bean风格用法.
 *
 * <p>{@code SimpleTrigger(Impl)}本身已经是一个JavaBean, 但缺乏合理的默认值.
 * 此类使用Spring bean名称作为作业名称, Quartz默认组 ("DEFAULT")作为作业组, 当前时间作为开始时间, 无限重复, 如果未指定.
 *
 * <p>该类还将使用给定的{@link org.quartz.JobDetail}的作业名称和组来注册触发器.
 * 这允许{@link SchedulerFactoryBean}自动注册相应JobDetail的触发器, 而不是单独注册JobDetail.
 */
public class SimpleTriggerFactoryBean implements FactoryBean<SimpleTrigger>, BeanNameAware, InitializingBean {

	/** Constants for the SimpleTrigger class */
	private static final Constants constants = new Constants(SimpleTrigger.class);


	private String name;

	private String group;

	private JobDetail jobDetail;

	private JobDataMap jobDataMap = new JobDataMap();

	private Date startTime;

	private long startDelay;

	private long repeatInterval;

	private int repeatCount = -1;

	private int priority;

	private int misfireInstruction;

	private String description;

	private String beanName;

	private SimpleTrigger simpleTrigger;


	/**
	 * 指定触发器的名称.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * 指定触发器的组.
	 */
	public void setGroup(String group) {
		this.group = group;
	}

	/**
	 * 设置此触发器应与之关联的JobDetail.
	 */
	public void setJobDetail(JobDetail jobDetail) {
		this.jobDetail = jobDetail;
	}

	/**
	 * 设置触发器的JobDataMap.
	 */
	public void setJobDataMap(JobDataMap jobDataMap) {
		this.jobDataMap = jobDataMap;
	}

	/**
	 * 返回触发器的JobDataMap.
	 */
	public JobDataMap getJobDataMap() {
		return this.jobDataMap;
	}

	/**
	 * 通过给定的Map在JobDataMap中注册对象.
	 * <p>与JobDetail数据Map中的对象相比, 这些对象仅可用于此Trigger.
	 * 
	 * @param jobDataAsMap 对象作为值(例如Spring管理的bean)
	 */
	public void setJobDataAsMap(Map<String, ?> jobDataAsMap) {
		this.jobDataMap.putAll(jobDataAsMap);
	}

	/**
	 * 设置触发器的特定开始时间.
	 * <p>请注意, 动态计算的{@link #setStartDelay}规范会覆盖此处设置的静态时间戳.
	 */
	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	/**
	 * 设置启动延迟, 以毫秒为单位.
	 * <p>启动延迟被添加到当前系统时间(当bean启动时)以控制触发器的开始时间.
	 */
	public void setStartDelay(long startDelay) {
		Assert.isTrue(startDelay >= 0, "Start delay cannot be negative");
		this.startDelay = startDelay;
	}

	/**
	 * 指定此触发器的执行时间间隔.
	 */
	public void setRepeatInterval(long repeatInterval) {
		this.repeatInterval = repeatInterval;
	}

	/**
	 * 指定此触发器应触发的次数.
	 * <p>默认是无限期重复.
	 */
	public void setRepeatCount(int repeatCount) {
		this.repeatCount = repeatCount;
	}

	/**
	 * 指定此触发器的优先级.
	 */
	public void setPriority(int priority) {
		this.priority = priority;
	}

	/**
	 * 指定此触发器的不触发指令.
	 */
	public void setMisfireInstruction(int misfireInstruction) {
		this.misfireInstruction = misfireInstruction;
	}

	/**
	 * 通过{@link org.quartz.SimpleTrigger}类中相应常量的名称, 设置不触发指令.
	 * 默认是{@code MISFIRE_INSTRUCTION_SMART_POLICY}.
	 */
	public void setMisfireInstructionName(String constantName) {
		this.misfireInstruction = constants.asNumber(constantName).intValue();
	}

	/**
	 * 将文本描述与此触发器相关联.
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}


	@Override
	public void afterPropertiesSet() {
		if (this.name == null) {
			this.name = this.beanName;
		}
		if (this.group == null) {
			this.group = Scheduler.DEFAULT_GROUP;
		}
		if (this.jobDetail != null) {
			this.jobDataMap.put("jobDetail", this.jobDetail);
		}
		if (this.startDelay > 0 || this.startTime == null) {
			this.startTime = new Date(System.currentTimeMillis() + this.startDelay);
		}

		SimpleTriggerImpl sti = new SimpleTriggerImpl();
		sti.setName(this.name);
		sti.setGroup(this.group);
		if (this.jobDetail != null) {
			sti.setJobKey(this.jobDetail.getKey());
		}
		sti.setJobDataMap(this.jobDataMap);
		sti.setStartTime(this.startTime);
		sti.setRepeatInterval(this.repeatInterval);
		sti.setRepeatCount(this.repeatCount);
		sti.setPriority(this.priority);
		sti.setMisfireInstruction(this.misfireInstruction);
		sti.setDescription(this.description);
		this.simpleTrigger = sti;
	}


	@Override
	public SimpleTrigger getObject() {
		return this.simpleTrigger;
	}

	@Override
	public Class<?> getObjectType() {
		return SimpleTrigger.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}
}
