package org.springframework.jmx.support;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.MBeanServerForwarder;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jmx.JmxException;
import org.springframework.util.CollectionUtils;

/**
 * 创建JSR-160 {@link JMXConnectorServer}的{@link FactoryBean}, 可选地将其注册到{@link MBeanServer}, 然后启动它.
 *
 * <p>通过将{@code threaded}属性设置为{@code true}, 可以在单独的线程中启动{@code JMXConnectorServer}.
 * 可以通过将{@code daemon}属性设置为 {@code true}来将此线程配置为守护程序线程.
 *
 * <p>当关闭{@code ApplicationContext}, 并销毁此类的实例时, {@code JMXConnectorServer}正确关闭.
 */
public class ConnectorServerFactoryBean extends MBeanRegistrationSupport
		implements FactoryBean<JMXConnectorServer>, InitializingBean, DisposableBean {

	/** 默认服务URL */
	public static final String DEFAULT_SERVICE_URL = "service:jmx:jmxmp://localhost:9875";


	private String serviceUrl = DEFAULT_SERVICE_URL;

	private Map<String, Object> environment = new HashMap<String, Object>();

	private MBeanServerForwarder forwarder;

	private ObjectName objectName;

	private boolean threaded = false;

	private boolean daemon = false;

	private JMXConnectorServer connectorServer;


	/**
	 * 设置{@code JMXConnectorServer}的服务URL.
	 */
	public void setServiceUrl(String serviceUrl) {
		this.serviceUrl = serviceUrl;
	}

	/**
	 * 设置用于构造{@code JMXConnectorServer}的环境属性 (String key/value pairs).
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
	 * 设置要应用于{@code JMXConnectorServer}的MBeanServerForwarder.
	 */
	public void setForwarder(MBeanServerForwarder forwarder) {
		this.forwarder = forwarder;
	}

	/**
	 * 设置{@code ObjectName}, 用于使用{@code MBeanServer}注册{@code JMXConnectorServer}本身,
	 * 作为{@code ObjectName}实例或作为{@code String}.
	 * 
	 * @throws MalformedObjectNameException 如果{@code ObjectName}格式错误
	 */
	public void setObjectName(Object objectName) throws MalformedObjectNameException {
		this.objectName = ObjectNameManager.getInstance(objectName);
	}

	/**
	 * 设置是否应在单独的线程中启动{@code JMXConnectorServer}.
	 */
	public void setThreaded(boolean threaded) {
		this.threaded = threaded;
	}

	/**
	 * 设置为{@code JMXConnectorServer}启动的线程是否应作为daemon线程启动.
	 */
	public void setDaemon(boolean daemon) {
		this.daemon = daemon;
	}


	/**
	 * 启动连接器服务器. 如果{@code threaded}标志设置为{@code true}, 则{@code JMXConnectorServer}将在单独的线程中启动.
	 * 如果{@code daemon}标志设置为{@code true}, 该线程将作为daemon线程启动.
	 * 
	 * @throws JMException 使用{@code MBeanServer}注册连接器服务器时出现问题
	 * @throws IOException 如果启动连接器服务器时出现问题
	 */
	@Override
	public void afterPropertiesSet() throws JMException, IOException {
		if (this.server == null) {
			this.server = JmxUtils.locateMBeanServer();
		}

		// Create the JMX service URL.
		JMXServiceURL url = new JMXServiceURL(this.serviceUrl);

		// Create the connector server now.
		this.connectorServer = JMXConnectorServerFactory.newJMXConnectorServer(url, this.environment, this.server);

		// Set the given MBeanServerForwarder, if any.
		if (this.forwarder != null) {
			this.connectorServer.setMBeanServerForwarder(this.forwarder);
		}

		// Do we want to register the connector with the MBean server?
		if (this.objectName != null) {
			doRegister(this.connectorServer, this.objectName);
		}

		try {
			if (this.threaded) {
				// Start the connector server asynchronously (in a separate thread).
				Thread connectorThread = new Thread() {
					@Override
					public void run() {
						try {
							connectorServer.start();
						}
						catch (IOException ex) {
							throw new JmxException("Could not start JMX connector server after delay", ex);
						}
					}
				};

				connectorThread.setName("JMX Connector Thread [" + this.serviceUrl + "]");
				connectorThread.setDaemon(this.daemon);
				connectorThread.start();
			}
			else {
				// Start the connector server in the same thread.
				this.connectorServer.start();
			}

			if (logger.isInfoEnabled()) {
				logger.info("JMX connector server started: " + this.connectorServer);
			}
		}

		catch (IOException ex) {
			// Unregister the connector server if startup failed.
			unregisterBeans();
			throw ex;
		}
	}


	@Override
	public JMXConnectorServer getObject() {
		return this.connectorServer;
	}

	@Override
	public Class<? extends JMXConnectorServer> getObjectType() {
		return (this.connectorServer != null ? this.connectorServer.getClass() : JMXConnectorServer.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}


	/**
	 * 停止由此类实例管理的{@code JMXConnectorServer}.
	 * 在{@code ApplicationContext}关闭时自动调用.
	 * 
	 * @throws IOException 如果停止连接器服务器时出错
	 */
	@Override
	public void destroy() throws IOException {
		if (logger.isInfoEnabled()) {
			logger.info("Stopping JMX connector server: " + this.connectorServer);
		}
		try {
			this.connectorServer.stop();
		}
		finally {
			unregisterBeans();
		}
	}
}
