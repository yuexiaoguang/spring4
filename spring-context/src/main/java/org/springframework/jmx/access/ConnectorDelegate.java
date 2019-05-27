package org.springframework.jmx.access;

import java.io.IOException;
import java.util.Map;
import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.jmx.MBeanServerNotFoundException;
import org.springframework.jmx.support.JmxUtils;

/**
 * 用于管理JMX连接器.
 */
class ConnectorDelegate {

	private static final Log logger = LogFactory.getLog(ConnectorDelegate.class);

	private JMXConnector connector;


	/**
	 * 使用配置的{@code JMXServiceURL}连接到远程{@code MBeanServer}:
	 * 到指定的JMX服务, 如果未指定服务URL, 则指向本地MBeanServer.
	 * 
	 * @param serviceUrl 要连接到的JMX服务URL (may be {@code null})
	 * @param environment 连接器的JMX环境 (may be {@code null})
	 * @param agentId 本地JMX MBeanServer的代理ID (may be {@code null})
	 */
	public MBeanServerConnection connect(JMXServiceURL serviceUrl, Map<String, ?> environment, String agentId)
			throws MBeanServerNotFoundException {

		if (serviceUrl != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Connecting to remote MBeanServer at URL [" + serviceUrl + "]");
			}
			try {
				this.connector = JMXConnectorFactory.connect(serviceUrl, environment);
				return this.connector.getMBeanServerConnection();
			}
			catch (IOException ex) {
				throw new MBeanServerNotFoundException("Could not connect to remote MBeanServer [" + serviceUrl + "]", ex);
			}
		}
		else {
			logger.debug("Attempting to locate local MBeanServer");
			return JmxUtils.locateMBeanServer(agentId);
		}
	}

	/**
	 * 关闭可能由此拦截器管理的{@code JMXConnector}.
	 */
	public void close() {
		if (this.connector != null) {
			try {
				this.connector.close();
			}
			catch (IOException ex) {
				logger.debug("Could not close JMX connector", ex);
			}
		}
	}

}
