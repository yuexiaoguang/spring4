package org.springframework.scheduling.quartz;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.Calendar;
import org.quartz.JobDetail;
import org.quartz.JobListener;
import org.quartz.ListenerManager;
import org.quartz.ObjectAlreadyExistsException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerListener;
import org.quartz.Trigger;
import org.quartz.TriggerListener;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.xml.XMLSchedulingDataProcessor;

import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * 用于访问Quartz Scheduler的公共基类, i.e. 用于在{@link org.quartz.Scheduler}实例上注册作业, 触发器和监听器.
 *
 * <p>有关具体用法, 请查看{@link SchedulerFactoryBean}和{@link SchedulerAccessorBean}类.
 *
 * <p>从Spring 4.1开始, 与Quartz 2.1.4及更高版本兼容.
 */
public abstract class SchedulerAccessor implements ResourceLoaderAware {

	protected final Log logger = LogFactory.getLog(getClass());

	private boolean overwriteExistingJobs = false;

	private String[] jobSchedulingDataLocations;

	private List<JobDetail> jobDetails;

	private Map<String, Calendar> calendars;

	private List<Trigger> triggers;

	private SchedulerListener[] schedulerListeners;

	private JobListener[] globalJobListeners;

	private TriggerListener[] globalTriggerListeners;

	private PlatformTransactionManager transactionManager;

	protected ResourceLoader resourceLoader;


	/**
	 * 设置此SchedulerFactoryBean上定义的作业是否应覆盖现有作业定义.
	 * 默认"false", 不覆盖已从永久性作业存储读入的已注册作业.
	 */
	public void setOverwriteExistingJobs(boolean overwriteExistingJobs) {
		this.overwriteExistingJobs = overwriteExistingJobs;
	}

	/**
	 * 设置Quartz作业定义XML文件的位置, 该文​​件位于"job_scheduling_data_1_5" XSD或更高版本之后.
	 * 可以指定自动注册在此类文件中定义的作业, 可能除了直接在此SchedulerFactoryBean上定义的作业外.
	 */
	public void setJobSchedulingDataLocation(String jobSchedulingDataLocation) {
		this.jobSchedulingDataLocations = new String[] {jobSchedulingDataLocation};
	}

	/**
	 * 设置Quartz作业定义XML文件的位置, 该文​​件位于"job_scheduling_data_1_5" XSD或更高版本之后.
	 * 可以指定自动注册在此类文件中定义的作业, 可能除了直接在此SchedulerFactoryBean上定义的作业外.
	 */
	public void setJobSchedulingDataLocations(String... jobSchedulingDataLocations) {
		this.jobSchedulingDataLocations = jobSchedulingDataLocations;
	}

	/**
	 * 使用此FactoryBean创建的Scheduler注册JobDetail对象, 以供Triggers引用.
	 * <p>当Trigger确定JobDetail本身时, 这不是必需的:
	 * 在这种情况下, JobDetail将与Trigger一起隐式注册.
	 */
	public void setJobDetails(JobDetail... jobDetails) {
		// 在这里使用可修改的ArrayList, 以允许在自动检测JobDetail感知触发器期间, 进一步添加JobDetail对象.
		this.jobDetails = new ArrayList<JobDetail>(Arrays.asList(jobDetails));
	}

	/**
	 * 使用此FactoryBean创建的Scheduler注册Quartz Calendar对象, 以供Triggers引用.
	 * 
	 * @param calendars 日历名称作为键, Calendar对象作为值
	 */
	public void setCalendars(Map<String, Calendar> calendars) {
		this.calendars = calendars;
	}

	/**
	 * 使用此FactoryBean创建的Scheduler注册Trigger对象.
	 * <p>如果Trigger确定相应的JobDetail本身, 则作业将自动注册到Scheduler.
	 * 否则, 需要通过此FactoryBean的"jobDetails"属性注册相应的JobDetail.
	 */
	public void setTriggers(Trigger... triggers) {
		this.triggers = Arrays.asList(triggers);
	}

	/**
	 * 指定要向Scheduler注册的Quartz SchedulerListeners.
	 */
	public void setSchedulerListeners(SchedulerListener... schedulerListeners) {
		this.schedulerListeners = schedulerListeners;
	}

	/**
	 * 指定要向Scheduler注册的全局Quartz JobListeners.
	 * 此类JobListeners将应用于Scheduler中的所有作业.
	 */
	public void setGlobalJobListeners(JobListener... globalJobListeners) {
		this.globalJobListeners = globalJobListeners;
	}

	/**
	 * 指定要向Scheduler注册的全局Quartz TriggerListeners.
	 * 此类TriggerListener将应用于Scheduler中的所有触发器.
	 */
	public void setGlobalTriggerListeners(TriggerListener... globalTriggerListeners) {
		this.globalTriggerListeners = globalTriggerListeners;
	}

	/**
	 * 设置事务管理器, 用于注册此SchedulerFactoryBean定义的作业和触发器.
	 * 默认无; 设置此选项仅在为Scheduler指定DataSource时才有意义.
	 */
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}


	/**
	 * 注册作业和触发器(如果可能, 在事务中).
	 */
	protected void registerJobsAndTriggers() throws SchedulerException {
		TransactionStatus transactionStatus = null;
		if (this.transactionManager != null) {
			transactionStatus = this.transactionManager.getTransaction(new DefaultTransactionDefinition());
		}

		try {
			if (this.jobSchedulingDataLocations != null) {
				ClassLoadHelper clh = new ResourceLoaderClassLoadHelper(this.resourceLoader);
				clh.initialize();
				XMLSchedulingDataProcessor dataProcessor = new XMLSchedulingDataProcessor(clh);
				for (String location : this.jobSchedulingDataLocations) {
					dataProcessor.processFileAndScheduleJobs(location, getScheduler());
				}
			}

			// Register JobDetails.
			if (this.jobDetails != null) {
				for (JobDetail jobDetail : this.jobDetails) {
					addJobToScheduler(jobDetail);
				}
			}
			else {
				// 创建空列表以便在注册触发器时更容易检查.
				this.jobDetails = new LinkedList<JobDetail>();
			}

			// Register Calendars.
			if (this.calendars != null) {
				for (String calendarName : this.calendars.keySet()) {
					Calendar calendar = this.calendars.get(calendarName);
					getScheduler().addCalendar(calendarName, calendar, true, true);
				}
			}

			// Register Triggers.
			if (this.triggers != null) {
				for (Trigger trigger : this.triggers) {
					addTriggerToScheduler(trigger);
				}
			}
		}

		catch (Throwable ex) {
			if (transactionStatus != null) {
				try {
					this.transactionManager.rollback(transactionStatus);
				}
				catch (TransactionException tex) {
					logger.error("Job registration exception overridden by rollback exception", ex);
					throw tex;
				}
			}
			if (ex instanceof SchedulerException) {
				throw (SchedulerException) ex;
			}
			if (ex instanceof Exception) {
				throw new SchedulerException("Registration of jobs and triggers failed: " + ex.getMessage(), ex);
			}
			throw new SchedulerException("Registration of jobs and triggers failed: " + ex.getMessage());
		}

		if (transactionStatus != null) {
			this.transactionManager.commit(transactionStatus);
		}
	}

	/**
	 * 将给定作业添加到Scheduler, 如果它尚不存在.
	 * 如果设置了 "overwriteExistingJobs", 则在任何情况下都会覆盖作业.
	 * 
	 * @param jobDetail 要添加的作业
	 * 
	 * @return {@code true}如果作业实际上已添加, {@code false}如果它之前已经存在
	 */
	private boolean addJobToScheduler(JobDetail jobDetail) throws SchedulerException {
		if (this.overwriteExistingJobs || getScheduler().getJobDetail(jobDetail.getKey()) == null) {
			getScheduler().addJob(jobDetail, true);
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * 将给定的触发器添加到Scheduler, 如果它尚不存在.
	 * 如果设置了"overwriteExistingJobs", 则在任何情况下都会覆盖触发器.
	 * 
	 * @param trigger 要添加的触发器
	 * 
	 * @return {@code true}如果触发器已实际添加, {@code false}如果它之前已经存在
	 */
	private boolean addTriggerToScheduler(Trigger trigger) throws SchedulerException {
		boolean triggerExists = (getScheduler().getTrigger(trigger.getKey()) != null);
		if (triggerExists && !this.overwriteExistingJobs) {
			return false;
		}

		// 检查触发器是否知道关联的JobDetail.
		JobDetail jobDetail = (JobDetail) trigger.getJobDataMap().remove("jobDetail");
		if (triggerExists) {
			if (jobDetail != null && !this.jobDetails.contains(jobDetail) && addJobToScheduler(jobDetail)) {
				this.jobDetails.add(jobDetail);
			}
			try {
				getScheduler().rescheduleJob(trigger.getKey(), trigger);
			}
			catch (ObjectAlreadyExistsException ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Unexpectedly encountered existing trigger on rescheduling, assumably due to " +
							"cluster race condition: " + ex.getMessage() + " - can safely be ignored");
				}
			}
		}
		else {
			try {
				if (jobDetail != null && !this.jobDetails.contains(jobDetail) &&
						(this.overwriteExistingJobs || getScheduler().getJobDetail(jobDetail.getKey()) == null)) {
					getScheduler().scheduleJob(jobDetail, trigger);
					this.jobDetails.add(jobDetail);
				}
				else {
					getScheduler().scheduleJob(trigger);
				}
			}
			catch (ObjectAlreadyExistsException ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Unexpectedly encountered existing trigger on job scheduling, assumably due to " +
							"cluster race condition: " + ex.getMessage() + " - can safely be ignored");
				}
				if (this.overwriteExistingJobs) {
					getScheduler().rescheduleJob(trigger.getKey(), trigger);
				}
			}
		}
		return true;
	}

	/**
	 * 使用Scheduler注册所有指定的监听器.
	 */
	protected void registerListeners() throws SchedulerException {
		ListenerManager listenerManager = getScheduler().getListenerManager();
		if (this.schedulerListeners != null) {
			for (SchedulerListener listener : this.schedulerListeners) {
				listenerManager.addSchedulerListener(listener);
			}
		}
		if (this.globalJobListeners != null) {
			for (JobListener listener : this.globalJobListeners) {
				listenerManager.addJobListener(listener);
			}
		}
		if (this.globalTriggerListeners != null) {
			for (TriggerListener listener : this.globalTriggerListeners) {
				listenerManager.addTriggerListener(listener);
			}
		}
	}


	/**
	 * 确定要对其进行操作的Scheduler的模板方法.
	 * 由子类实现.
	 */
	protected abstract Scheduler getScheduler();

}
