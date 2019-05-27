package org.springframework.jmx.support;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.target.AbstractLazyCreationTargetSource;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

/**
 * 在通过{@code JMXServerConnector}公开的远程{@code MBeanServer}上创建JMX 1.2 {@code MBeanServerConnection}的{@link FactoryBean}.
 * 公开{@code MBeanServer}以获取bean引用.
 */
public class MBeanServerConnectionFactoryBean
		implements FactoryBean<MBeanServerConnection>, BeanClassLoaderAware, InitializingBean, DisposableBean {

	private JMXServiceURL serviceUrl;

	private Map<String, Object> environment = new HashMap<String, Object>();

	private boolean connectOnStartup = true;

	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	private JMXConnector connector;

	private MBeanServerConnection connection;

	private JMXConnectorLazyInitTargetSource connectorTargetSource;


	/**
	 * 设置远程{@code MBeanServer}的服务URL.
	 */
	public void setServiceUrl(String url) throws MalformedURLException {
		this.serviceUrl = new JMXServiceURL(url);
	}

	/**
	 * 设置用于构造{@code JMXConnector}的环境属性 (String key/value pairs).
	 */
	public void setEnvironment(Properties environment) {
		CollectionUtils.mergePropertiesIntoMap(environment, this.environment);
	}

	/**
	 * 设置用于构造{@code JMXConnector}的环境属性.
	 */
	public void setEnvironmentMap(Map<String, ?> environment) {
		if (environment != null) {
			this.environment.putAll(environment);
		}
	}

	/**
	 * 设置是否在启动时连接到服务器. 默认"true".
	 * <p>可以关闭以允许JMX服务器延迟启动.
	 * 在这种情况下, 将在首次访问时获取JMX连接器.
	 */
	public void setConnectOnStartup(boolean connectOnStartup) {
		this.connectOnStartup = connectOnStartup;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}


	/**
	 * 为给定设置创建{@code JMXConnector}, 并公开关联的{@code MBeanServerConnection}.
	 */
	@Override
	public void afterPropertiesSet() throws IOException {
		if (this.serviceUrl == null) {
			throw new IllegalArgumentException("Property 'serviceUrl' is required");
		}

		if (this.connectOnStartup) {
			connect();
		}
		else {
			createLazyConnection();
		}
	}

	/**
	 * 使用配置的服务URL和环境属性连接到远程{@code MBeanServer}.
	 */
	private void connect() throws IOException {
		this.connector = JMXConnectorFactory.connect(this.serviceUrl, this.environment);
		this.connection = this.connector.getMBeanServerConnection();
	}

	/**
	 * 为{@code JMXConnector}和{@code MBeanServerConnection}创建延迟代理
	 */
	private void createLazyConnection() {
		this.connectorTargetSource = new JMXConnectorLazyInitTargetSource();
		TargetSource connectionTargetSource = new MBeanServerConnectionLazyInitTargetSource();

		this.connector = (JMXConnector)
				new ProxyFactory(JMXConnector.class, this.connectorTargetSource).getProxy(this.beanClassLoader);
		this.connection = (MBeanServerConnection)
				new ProxyFactory(MBeanServerConnection.class, connectionTargetSource).getProxy(this.beanClassLoader);
	}


	@Override
	public MBeanServerConnection getObject() {
		return this.connection;
	}

	@Override
	public Class<? extends MBeanServerConnection> getObjectType() {
		return (this.connection != null ? this.connection.getClass() : MBeanServerConnection.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}


	/**
	 * 关闭底层{@code JMXConnector}.
	 */
	@Override
	public void destroy() throws IOException {
		if (this.connectorTargetSource == null || this.connectorTargetSource.isInitialized()) {
			this.connector.close();
		}
	}


	/**
	 * 使用配置的服务URL和环境属性延迟创建{@code JMXConnector}.
	 */
	private class JMXConnectorLazyInitTargetSource extends AbstractLazyCreationTargetSource {

		@Override
		protected Object createObject() throws Exception {
			return JMXConnectorFactory.connect(serviceUrl, environment);
		}

		@Override
		public Class<?> getTargetClass() {
			return JMXConnector.class;
		}
	}


	/**
	 * 延迟创建{@code MBeanServerConnection}.
	 */
	private class MBeanServerConnectionLazyInitTargetSource extends AbstractLazyCreationTargetSource {

		@Override
		protected Object createObject() throws Exception {
			return connector.getMBeanServerConnection();
		}

		@Override
		public Class<?> getTargetClass() {
			return MBeanServerConnection.class;
		}
	}
}
