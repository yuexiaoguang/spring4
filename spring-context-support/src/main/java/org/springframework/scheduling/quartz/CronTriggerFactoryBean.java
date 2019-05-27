package org.springframework.scheduling.quartz;

import java.text.ParseException;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.impl.triggers.CronTriggerImpl;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.Constants;
import org.springframework.util.Assert;

/**
 * Spring {@link FactoryBean}用于创建Quartz {@link org.quartz.CronTrigger}实例, 支持触发器配置的bean风格用法.
 *
 * <p>{@code CronTrigger(Impl)}本身已经是一个JavaBean, 但缺乏合理的默认值.
 * 此类使用Spring bean名称作为作业名称, Quartz默认组 ("DEFAULT")作为作业组, 当前时间作为开始时间, 无限重复, 如果未指定.
 *
 * <p>该类还将使用给定的{@link org.quartz.JobDetail}的作业名称和组来注册触发器.
 * 这允许{@link SchedulerFactoryBean}自动注册相应JobDetail的触发器, 而不是单独注册JobDetail.
 */
public class CronTriggerFactoryBean implements FactoryBean<CronTrigger>, BeanNameAware, InitializingBean {

	/** Constants for the CronTrigger class */
	private static final Constants constants = new Constants(CronTrigger.class);


	private String name;

	private String group;

	private JobDetail jobDetail;

	private JobDataMap jobDataMap = new JobDataMap();

	private Date startTime;

	private long startDelay = 0;

	private String cronExpression;

	private TimeZone timeZone;

	private String calendarName;

	private int priority;

	private int misfireInstruction;

	private String description;

	private String beanName;

	private CronTrigger cronTrigger;


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
	 * <p>与JobDetail数据映射中的对象相比, 这些对象仅可用于此Trigger.
	 * 
	 * @param jobDataAsMap 任何对象作为值 (例如Spring管理的bean)
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
	 * <p>启动延迟被添加到当前系统时间 (当bean启动时) 以控制触发器的开始时间.
	 */
	public void setStartDelay(long startDelay) {
		Assert.isTrue(startDelay >= 0, "Start delay cannot be negative");
		this.startDelay = startDelay;
	}

	/**
	 * 指定此触发器的cron表达式.
	 */
	public void setCronExpression(String cronExpression) {
		this.cronExpression = cronExpression;
	}

	/**
	 * 指定此触发器的cron表达式的时区.
	 */
	public void setTimeZone(TimeZone timeZone) {
		this.timeZone = timeZone;
	}

	/**
	 * 将特定日历与此cron触发器相关联.
	 */
	public void setCalendarName(String calendarName) {
		this.calendarName = calendarName;
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
	 * 通过{@link org.quartz.CronTrigger}类中相应常量的名称设置misfire指令.
	 * 默认{@code MISFIRE_INSTRUCTION_SMART_POLICY}.
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
	public void afterPropertiesSet() throws ParseException {
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
		if (this.timeZone == null) {
			this.timeZone = TimeZone.getDefault();
		}

		CronTriggerImpl cti = new CronTriggerImpl();
		cti.setName(this.name);
		cti.setGroup(this.group);
		if (this.jobDetail != null) {
			cti.setJobKey(this.jobDetail.getKey());
		}
		cti.setJobDataMap(this.jobDataMap);
		cti.setStartTime(this.startTime);
		cti.setCronExpression(this.cronExpression);
		cti.setTimeZone(this.timeZone);
		cti.setCalendarName(this.calendarName);
		cti.setPriority(this.priority);
		cti.setMisfireInstruction(this.misfireInstruction);
		cti.setDescription(this.description);
		this.cronTrigger = cti;
	}


	@Override
	public CronTrigger getObject() {
		return this.cronTrigger;
	}

	@Override
	public Class<?> getObjectType() {
		return CronTrigger.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
