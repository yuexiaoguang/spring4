package org.springframework.jca.work.jboss;

import javax.resource.spi.work.WorkManager;

import org.springframework.jca.work.WorkManagerTaskExecutor;

/**
 * Spring TaskExecutor adapter for the JBoss JCA WorkManager.
 * Can be defined in web applications to make a TaskExecutor reference
 * available, talking to the JBoss WorkManager (thread pool) underneath.
 *
 * <p>This is the JBoss equivalent of the CommonJ
 * {@link org.springframework.scheduling.commonj.WorkManagerTaskExecutor}
 * adapter for WebLogic and WebSphere.
 *
 * <p>This class does not work on JBoss 7 or higher. There is no known
 * immediate replacement, since JBoss does not want its JCA WorkManager
 * to be exposed anymore. As of JBoss/WildFly 8, a
 * {@link org.springframework.scheduling.concurrent.DefaultManagedTaskExecutor}
 * may be used, following JSR-236 support in Java EE 7.
 *
 * @deprecated as of Spring 4.0, since there are no fully supported versions
 * of JBoss that this class works with anymore
 */
@Deprecated
public class JBossWorkManagerTaskExecutor extends WorkManagerTaskExecutor {

	/**
	 * Identify a specific JBossWorkManagerMBean to talk to,
	 * through its JMX object name.
	 * <p>The default MBean name is "jboss.jca:service=WorkManager".
	 */
	public void setWorkManagerMBeanName(String mbeanName) {
		setWorkManager(JBossWorkManagerUtils.getWorkManager(mbeanName));
	}

	/**
	 * Obtains the default JBoss JCA WorkManager through a JMX lookup
	 * for the JBossWorkManagerMBean.
	 * @see JBossWorkManagerUtils#getWorkManager()
	 */
	@Override
	protected WorkManager getDefaultWorkManager() {
		return JBossWorkManagerUtils.getWorkManager();
	}

}
