package org.springframework.jca.work.jboss;

import java.lang.reflect.Method;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;
import javax.naming.InitialContext;
import javax.resource.spi.work.WorkManager;

import org.springframework.util.Assert;

/**
 * Utility class for obtaining the JBoss JCA WorkManager,
 * typically for use in web applications.
 *
 * @deprecated as of Spring 4.0, since there are no fully supported versions
 * of JBoss that this class works with anymore
 */
@Deprecated
public abstract class JBossWorkManagerUtils {

	private static final String JBOSS_WORK_MANAGER_MBEAN_CLASS_NAME = "org.jboss.resource.work.JBossWorkManagerMBean";

	private static final String MBEAN_SERVER_CONNECTION_JNDI_NAME = "jmx/invoker/RMIAdaptor";

	private static final String DEFAULT_WORK_MANAGER_MBEAN_NAME = "jboss.jca:service=WorkManager";


	/**
	 * Obtain the default JBoss JCA WorkManager through a JMX lookup
	 * for the default JBossWorkManagerMBean.
	 * @see org.jboss.resource.work.JBossWorkManagerMBean
	 */
	public static WorkManager getWorkManager() {
		return getWorkManager(DEFAULT_WORK_MANAGER_MBEAN_NAME);
	}

	/**
	 * Obtain the default JBoss JCA WorkManager through a JMX lookup
	 * for the JBossWorkManagerMBean.
	 * @param mbeanName the JMX object name to use
	 * @see org.jboss.resource.work.JBossWorkManagerMBean
	 */
	public static WorkManager getWorkManager(String mbeanName) {
		Assert.hasLength(mbeanName, "JBossWorkManagerMBean name must not be empty");
		try {
			Class<?> mbeanClass = JBossWorkManagerUtils.class.getClassLoader().loadClass(JBOSS_WORK_MANAGER_MBEAN_CLASS_NAME);
			InitialContext jndiContext = new InitialContext();
			MBeanServerConnection mconn = (MBeanServerConnection) jndiContext.lookup(MBEAN_SERVER_CONNECTION_JNDI_NAME);
			ObjectName objectName = ObjectName.getInstance(mbeanName);
			Object workManagerMBean = MBeanServerInvocationHandler.newProxyInstance(mconn, objectName, mbeanClass, false);
			Method getInstanceMethod = workManagerMBean.getClass().getMethod("getInstance");
			return (WorkManager) getInstanceMethod.invoke(workManagerMBean);
		}
		catch (Exception ex) {
			throw new IllegalStateException(
					"Could not initialize JBossWorkManagerTaskExecutor because JBoss API is not available", ex);
		}
	}

}
