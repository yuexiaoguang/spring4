package org.springframework.jmx.support;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javax.management.MBeanServer;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jmx.MBeanServerNotFoundException;

/**
 * 通过WebSphere的专有{@code AdminServiceFactory} API获取WebSphere {@link javax.management.MBeanServer}引用的{@link FactoryBean}
 * 该API在WebSphere 5.1及更高版本上可用.
 *
 * <p>公开{@code MBeanServer}以获取bean引用.
 * 此FactoryBean是{@link MBeanServerFactoryBean}的直接替代, 它使用标准JMX 1.2 API访问平台的MBeanServer.
 *
 * <p>See the javadocs for WebSphere's
 * <a href="http://bit.ly/UzccDt">{@code AdminServiceFactory}</a>
 * and <a href="http://bit.ly/TRlX2r">{@code MBeanFactory}</a>.
 */
public class WebSphereMBeanServerFactoryBean implements FactoryBean<MBeanServer>, InitializingBean {

	private static final String ADMIN_SERVICE_FACTORY_CLASS = "com.ibm.websphere.management.AdminServiceFactory";

	private static final String GET_MBEAN_FACTORY_METHOD = "getMBeanFactory";

	private static final String GET_MBEAN_SERVER_METHOD = "getMBeanServer";


	private MBeanServer mbeanServer;


	@Override
	public void afterPropertiesSet() throws MBeanServerNotFoundException {
		try {
			/*
			 * this.mbeanServer = AdminServiceFactory.getMBeanFactory().getMBeanServer();
			 */
			Class<?> adminServiceClass = getClass().getClassLoader().loadClass(ADMIN_SERVICE_FACTORY_CLASS);
			Method getMBeanFactoryMethod = adminServiceClass.getMethod(GET_MBEAN_FACTORY_METHOD);
			Object mbeanFactory = getMBeanFactoryMethod.invoke(null);
			Method getMBeanServerMethod = mbeanFactory.getClass().getMethod(GET_MBEAN_SERVER_METHOD);
			this.mbeanServer = (MBeanServer) getMBeanServerMethod.invoke(mbeanFactory);
		}
		catch (ClassNotFoundException ex) {
			throw new MBeanServerNotFoundException("Could not find WebSphere's AdminServiceFactory class", ex);
		}
		catch (InvocationTargetException ex) {
			throw new MBeanServerNotFoundException(
					"WebSphere's AdminServiceFactory.getMBeanFactory/getMBeanServer method failed", ex.getTargetException());
		}
		catch (Exception ex) {
			throw new MBeanServerNotFoundException(
					"Could not access WebSphere's AdminServiceFactory.getMBeanFactory/getMBeanServer method", ex);
		}
	}


	@Override
	public MBeanServer getObject() {
		return this.mbeanServer;
	}

	@Override
	public Class<? extends MBeanServer> getObjectType() {
		return (this.mbeanServer != null ? this.mbeanServer.getClass() : MBeanServer.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
