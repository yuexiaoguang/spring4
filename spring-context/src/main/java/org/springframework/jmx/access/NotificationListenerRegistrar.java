package org.springframework.jmx.access;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Map;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXServiceURL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jmx.JmxException;
import org.springframework.jmx.MBeanServerNotFoundException;
import org.springframework.jmx.support.NotificationListenerHolder;
import org.springframework.util.CollectionUtils;

/**
 * 注册器对象, 它将特定的{@link javax.management.NotificationListener}
 * 与{@link javax.management.MBeanServer}中的一个或多个MBean相关联
 * (通常通过 {@link javax.management.MBeanServerConnection}).
 */
public class NotificationListenerRegistrar extends NotificationListenerHolder
		implements InitializingBean, DisposableBean {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private MBeanServerConnection server;

	private JMXServiceURL serviceUrl;

	private Map<String, ?> environment;

	private String agentId;

	private final ConnectorDelegate connector = new ConnectorDelegate();

	private ObjectName[] actualObjectNames;


	/**
	 * 设置用于连接所有调用都将路由到的MBean的{@code MBeanServerConnection}.
	 */
	public void setServer(MBeanServerConnection server) {
		this.server = server;
	}

	/**
	 * 指定JMX连接器的环境.
	 */
	public void setEnvironment(Map<String, ?> environment) {
		this.environment = environment;
	}

	/**
	 * 允许访问为连接器设置的环境, 并添加或覆盖特定条目.
	 * <p>用于直接指定条目, 例如通过 "environment[myKey]".
	 * 这对于在子bean定义中添加或覆盖条目特别有用.
	 */
	public Map<String, ?> getEnvironment() {
		return this.environment;
	}

	/**
	 * 设置远程{@code MBeanServer}的服务URL.
	 */
	public void setServiceUrl(String url) throws MalformedURLException {
		this.serviceUrl = new JMXServiceURL(url);
	}

	/**
	 * 设置{@code MBeanServer}的代理ID以进行定位.
	 * <p>默认无. 如果指定, 这将导致尝试定位助理MBeanServer, 除非已设置{@link #setServiceUrl "serviceUrl"}属性.
	 * <p>指定空字符串表示平台MBeanServer.
	 */
	public void setAgentId(String agentId) {
		this.agentId = agentId;
	}


	@Override
	public void afterPropertiesSet() {
		if (getNotificationListener() == null) {
			throw new IllegalArgumentException("Property 'notificationListener' is required");
		}
		if (CollectionUtils.isEmpty(this.mappedObjectNames)) {
			throw new IllegalArgumentException("Property 'mappedObjectName' is required");
		}
		prepare();
	}

	/**
	 * 注册指定的 {@code NotificationListener}.
	 * <p>确保配置了{@code MBeanServerConnection}, 并尝试检测本地连接, 如果未提供本地连接.
	 */
	public void prepare() {
		if (this.server == null) {
			this.server = this.connector.connect(this.serviceUrl, this.environment, this.agentId);
		}
		try {
			this.actualObjectNames = getResolvedObjectNames();
			if (logger.isDebugEnabled()) {
				logger.debug("Registering NotificationListener for MBeans " + Arrays.asList(this.actualObjectNames));
			}
			for (ObjectName actualObjectName : this.actualObjectNames) {
				this.server.addNotificationListener(
						actualObjectName, getNotificationListener(), getNotificationFilter(), getHandback());
			}
		}
		catch (IOException ex) {
			throw new MBeanServerNotFoundException(
					"Could not connect to remote MBeanServer at URL [" + this.serviceUrl + "]", ex);
		}
		catch (Exception ex) {
			throw new JmxException("Unable to register NotificationListener", ex);
		}
	}

	/**
	 * 取消注册指定的 {@code NotificationListener}.
	 */
	@Override
	public void destroy() {
		try {
			if (this.actualObjectNames != null) {
				for (ObjectName actualObjectName : this.actualObjectNames) {
					try {
						this.server.removeNotificationListener(
								actualObjectName, getNotificationListener(), getNotificationFilter(), getHandback());
					}
					catch (Exception ex) {
						if (logger.isDebugEnabled()) {
							logger.debug("Unable to unregister NotificationListener", ex);
						}
					}
				}
			}
		}
		finally {
			this.connector.close();
		}
	}
}
