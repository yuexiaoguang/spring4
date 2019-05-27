package org.springframework.scheduling.commonj;

import javax.naming.NamingException;

import commonj.timers.TimerManager;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.Lifecycle;
import org.springframework.jndi.JndiLocatorSupport;

/**
 * 访问CommonJ {@link commonj.timers.TimerManager}的类的基类.
 * 定义常见配置设置和常见生命周期处理.
 */
public abstract class TimerManagerAccessor extends JndiLocatorSupport
		implements InitializingBean, DisposableBean, Lifecycle {

	private TimerManager timerManager;

	private String timerManagerName;

	private boolean shared = false;


	/**
	 * 指定要委托给的CommonJ TimerManager.
	 * <p>请注意, 给定的TimerManager的生命周期将由此FactoryBean管理.
	 * <p>或者(通常), 可以指定目标TimerManager的JNDI名称.
	 */
	public void setTimerManager(TimerManager timerManager) {
		this.timerManager = timerManager;
	}

	/**
	 * 设置CommonJ TimerManager的JNDI名称.
	 * <p>如果"resourceRef"设置为"true", 则可以是完全限定的JNDI名称, 也可以是相对于当前环境命名上下文的JNDI名称.
	 */
	public void setTimerManagerName(String timerManagerName) {
		this.timerManagerName = timerManagerName;
	}

	/**
	 * 指定此FactoryBean获取的TimerManager是共享实例 ("true") 还是独立实例("false").
	 * 前者的生命周期应该由应用程序服务器管理, 而后者的生命周期由应用程序决定.
	 * <p>默认"false", i.e. 管理一个独立的TimerManager实例.
	 * 这就是CommonJ规范建议应用服务器通过JNDI查找提供的内容,
	 * 通常在{@code web.xml}中声明为{@code commonj.timers.TimerManager}类型的{@code resource-ref},
	 * 并且{@code res-sharing-scope}设置为 'Unshareable'.
	 * <p>将此标志切换为"true", 如果要获取共享的TimerManager, 通常通过指定已明确声明为'Shareable'的TimerManager的JNDI位置.
	 * 请注意, WebLogic的集群感知Job Scheduler也是共享的TimerManager.
	 * <p>这个FactoryBean处于共享或非共享模式的唯一区别,
	 * 是它只会在独立(非共享)实例的情况下, 尝试挂起/恢复/停止底层TimerManager.
	 * 这仅影响{@link org.springframework.context.Lifecycle}支持以及应用程序上下文关闭.
	 */
	public void setShared(boolean shared) {
		this.shared = shared;
	}


	@Override
	public void afterPropertiesSet() throws NamingException {
		if (this.timerManager == null) {
			if (this.timerManagerName == null) {
				throw new IllegalArgumentException("Either 'timerManager' or 'timerManagerName' must be specified");
			}
			this.timerManager = lookup(this.timerManagerName, TimerManager.class);
		}
	}

	protected final TimerManager getTimerManager() {
		return this.timerManager;
	}


	//---------------------------------------------------------------------
	// Implementation of Lifecycle interface
	//---------------------------------------------------------------------

	/**
	 * 恢复底层TimerManager (如果不共享).
	 */
	@Override
	public void start() {
		if (!this.shared) {
			this.timerManager.resume();
		}
	}

	/**
	 * 暂停底层TimerManager (如果不共享).
	 */
	@Override
	public void stop() {
		if (!this.shared) {
			this.timerManager.suspend();
		}
	}

	/**
	 * 如果底层TimerManager既不挂起也不停止, 则认为它是正在运行的.
	 */
	@Override
	public boolean isRunning() {
		return (!this.timerManager.isSuspending() && !this.timerManager.isStopping());
	}


	//---------------------------------------------------------------------
	// Implementation of DisposableBean interface
	//---------------------------------------------------------------------

	/**
	 * 停止底层TimerManager (如果不共享).
	 */
	@Override
	public void destroy() {
		// 停止整个TimerManager.
		if (!this.shared) {
			// 可能会早点返回, 但至少我们已经取消了所有已知的计时器.
			this.timerManager.stop();
		}
	}

}
