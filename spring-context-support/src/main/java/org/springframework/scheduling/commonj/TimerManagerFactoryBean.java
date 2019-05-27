package org.springframework.scheduling.commonj;

import java.util.LinkedList;
import java.util.List;
import javax.naming.NamingException;

import commonj.timers.Timer;
import commonj.timers.TimerManager;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.Lifecycle;

/**
 * {@link org.springframework.beans.factory.FactoryBean}检索CommonJ {@link commonj.timers.TimerManager}并公开它以获取bean引用.
 *
 * <p><b>这是在Spring上下文中设置CommonJ TimerManager的中心便利类.</b>
 *
 * <p>允许注册ScheduledTimerListeners.
 * 这是此类的主要目的; TimerManager本身也可以通过{@link org.springframework.jndi.JndiObjectFactoryBean}从JNDI获取.
 * 在只需要在启动时静态注册任务的场景中, 无需在应用程序代码中访问TimerManager本身.
 *
 * <p>请注意, TimerManager使用在重复执行之间共享的TimerListener实例, 而Quartz则为每次执行实例化一个新Job.
 */
public class TimerManagerFactoryBean extends TimerManagerAccessor
		implements FactoryBean<TimerManager>, InitializingBean, DisposableBean, Lifecycle {

	private ScheduledTimerListener[] scheduledTimerListeners;

	private final List<Timer> timers = new LinkedList<Timer>();


	/**
	 * 使用此FactoryBean创建的TimerManager注册ScheduledTimerListener对象的列表.
	 * 根据每个ScheduledTimerListener的设置, 它将通过TimerManager的一个调度方法进行注册.
	 */
	public void setScheduledTimerListeners(ScheduledTimerListener[] scheduledTimerListeners) {
		this.scheduledTimerListeners = scheduledTimerListeners;
	}


	//---------------------------------------------------------------------
	// Implementation of InitializingBean interface
	//---------------------------------------------------------------------

	@Override
	public void afterPropertiesSet() throws NamingException {
		super.afterPropertiesSet();
		if (this.scheduledTimerListeners != null) {
			TimerManager timerManager = getTimerManager();
			for (ScheduledTimerListener scheduledTask : this.scheduledTimerListeners) {
				Timer timer;
				if (scheduledTask.isOneTimeTask()) {
					timer = timerManager.schedule(scheduledTask.getTimerListener(), scheduledTask.getDelay());
				}
				else {
					if (scheduledTask.isFixedRate()) {
						timer = timerManager.scheduleAtFixedRate(
								scheduledTask.getTimerListener(), scheduledTask.getDelay(), scheduledTask.getPeriod());
					}
					else {
						timer = timerManager.schedule(
								scheduledTask.getTimerListener(), scheduledTask.getDelay(), scheduledTask.getPeriod());
					}
				}
				this.timers.add(timer);
			}
		}
	}


	//---------------------------------------------------------------------
	// Implementation of FactoryBean interface
	//---------------------------------------------------------------------

	@Override
	public TimerManager getObject() {
		return getTimerManager();
	}

	@Override
	public Class<? extends TimerManager> getObjectType() {
		TimerManager timerManager = getTimerManager();
		return (timerManager != null ? timerManager.getClass() : TimerManager.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}


	//---------------------------------------------------------------------
	// Implementation of DisposableBean interface
	//---------------------------------------------------------------------

	/**
	 * 在关闭时取消所有静态注册的计时器, 并停止底层TimerManager (如果不共享).
	 */
	@Override
	public void destroy() {
		// Cancel all registered timers.
		for (Timer timer : this.timers) {
			try {
				timer.cancel();
			}
			catch (Throwable ex) {
				logger.warn("Could not cancel CommonJ Timer", ex);
			}
		}
		this.timers.clear();

		// Stop the TimerManager itself.
		super.destroy();
	}

}
