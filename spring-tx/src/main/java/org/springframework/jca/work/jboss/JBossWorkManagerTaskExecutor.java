package org.springframework.jca.work.jboss;

import javax.resource.spi.work.WorkManager;

import org.springframework.jca.work.WorkManagerTaskExecutor;

/**
 * 用于JBoss JCA WorkManager的Spring TaskExecutor适配器.
 * 可以在Web应用程序中定义以使TaskExecutor引用可用, 与下面的JBoss WorkManager(线程池)通信.
 *
 * <p>这是用于WebLogic和WebSphere的
 * CommonJ {@link org.springframework.scheduling.commonj.WorkManagerTaskExecutor}适配器的JBoss等价物.
 *
 * <p>这个类不适用于JBoss 7或更高版本. 没有已知的立即替换, 因为JBoss不再希望其JCA WorkManager被暴露.
 * 从JBoss/WildFly 8开始, 在Java EE 7中支持JSR-236之后,
 * 可以使用{@link org.springframework.scheduling.concurrent.DefaultManagedTaskExecutor}.
 *
 * @deprecated 从Spring 4.0开始, 因为没有完全支持的JBoss版本, 所以这个类不再适用了
 */
@Deprecated
public class JBossWorkManagerTaskExecutor extends WorkManagerTaskExecutor {

	/**
	 * 通过其JMX对象名称标识要与之通信的特定JBossWorkManagerMBean.
	 * <p>默认的 MBean名称是"jboss.jca:service=WorkManager".
	 */
	public void setWorkManagerMBeanName(String mbeanName) {
		setWorkManager(JBossWorkManagerUtils.getWorkManager(mbeanName));
	}

	/**
	 * 通过JBossWorkManagerMBean的JMX查找获得默认的JBoss JCA WorkManager.
	 */
	@Override
	protected WorkManager getDefaultWorkManager() {
		return JBossWorkManagerUtils.getWorkManager();
	}

}
