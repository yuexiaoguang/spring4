package org.springframework.jca.work.jboss;

import java.lang.reflect.Method;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;
import javax.naming.InitialContext;
import javax.resource.spi.work.WorkManager;

import org.springframework.util.Assert;

/**
 * 用于获取JBoss JCA WorkManager的工具类, 通常用于Web应用程序.
 *
 * @deprecated 从Spring 4.0开始, 因为没有完全支持的JBoss版本, 所以这个类不再适用了
 */
@Deprecated
public abstract class JBossWorkManagerUtils {

	private static final String JBOSS_WORK_MANAGER_MBEAN_CLASS_NAME = "org.jboss.resource.work.JBossWorkManagerMBean";

	private static final String MBEAN_SERVER_CONNECTION_JNDI_NAME = "jmx/invoker/RMIAdaptor";

	private static final String DEFAULT_WORK_MANAGER_MBEAN_NAME = "jboss.jca:service=WorkManager";


	/**
	 * 通过JMX查找默认的JBossWorkManagerMBean获取默认的JBoss JCA WorkManager.
	 */
	public static WorkManager getWorkManager() {
		return getWorkManager(DEFAULT_WORK_MANAGER_MBEAN_NAME);
	}

	/**
	 * 通过JBossWorkManagerMBean的JMX查找获取默认的JBoss JCA WorkManager.
	 * 
	 * @param mbeanName 要使用的JMX对象名称
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
