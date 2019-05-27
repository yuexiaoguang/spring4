package org.springframework.scheduling.quartz;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.SchedulerRepository;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ListableBeanFactory;

/**
 * 用于访问Quartz Scheduler的Spring bean风格的类,
 * i.e. 用于在给定的{@link org.quartz.Scheduler}实例上注册作业, 触发器和监听器.
 *
 * <p>从Spring 4.1开始, 与Quartz 2.1.4及更高版本兼容.
 */
public class SchedulerAccessorBean extends SchedulerAccessor implements BeanFactoryAware, InitializingBean {

	private String schedulerName;

	private Scheduler scheduler;

	private BeanFactory beanFactory;


	/**
	 * 指定Quartz {@link Scheduler}, 以通过Spring应用程序上下文或Quartz {@link org.quartz.impl.SchedulerRepository}中的调度程序名称进行操作.
	 * <p>可以通过自定义引导在存储库中注册调度器,
	 * e.g. 通过{@link org.quartz.impl.StdSchedulerFactory}或{@link org.quartz.impl.DirectSchedulerFactory}工厂类.
	 * 但是, 一般来说, 最好使用Spring的{@link SchedulerFactoryBean}, 它包括此访问器的作业/触发器/监听器功能.
	 * <p>如果未指定, 此访问器将尝试从包含的应用程序上下文中检索默认的{@link Scheduler} bean.
	 */
	public void setSchedulerName(String schedulerName) {
		this.schedulerName = schedulerName;
	}

	/**
	 * 指定要操作的Quartz {@link Scheduler}实例.
	 * <p>如果未指定, 此访问器将尝试从包含的应用程序上下文中检索默认的{@link Scheduler} bean.
	 */
	public void setScheduler(Scheduler scheduler) {
		this.scheduler = scheduler;
	}

	/**
	 * 返回此访问器操作的Quartz Scheduler实例.
	 */
	@Override
	public Scheduler getScheduler() {
		return this.scheduler;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}


	@Override
	public void afterPropertiesSet() throws SchedulerException {
		if (this.scheduler == null) {
			this.scheduler = (this.schedulerName != null ? findScheduler(this.schedulerName) : findDefaultScheduler());
		}
		registerListeners();
		registerJobsAndTriggers();
	}

	protected Scheduler findScheduler(String schedulerName) throws SchedulerException {
		if (this.beanFactory instanceof ListableBeanFactory) {
			ListableBeanFactory lbf = (ListableBeanFactory) this.beanFactory;
			String[] beanNames = lbf.getBeanNamesForType(Scheduler.class);
			for (String beanName : beanNames) {
				Scheduler schedulerBean = (Scheduler) lbf.getBean(beanName);
				if (schedulerName.equals(schedulerBean.getSchedulerName())) {
					return schedulerBean;
				}
			}
		}
		Scheduler schedulerInRepo = SchedulerRepository.getInstance().lookup(schedulerName);
		if (schedulerInRepo == null) {
			throw new IllegalStateException("No Scheduler named '" + schedulerName + "' found");
		}
		return schedulerInRepo;
	}

	protected Scheduler findDefaultScheduler() {
		if (this.beanFactory != null) {
			return this.beanFactory.getBean(Scheduler.class);
		}
		else {
			throw new IllegalStateException(
					"No Scheduler specified, and cannot find a default Scheduler without a BeanFactory");
		}
	}
}
