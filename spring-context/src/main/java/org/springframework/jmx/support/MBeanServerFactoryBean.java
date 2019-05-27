package org.springframework.jmx.support;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jmx.MBeanServerNotFoundException;

/**
 * 通过标准JMX 1.2 {@link javax.management.MBeanServerFactory} API (可在JDK 1.5上或作为JMX 1.2提供者的一部分获得)
 * 获取{@link javax.management.MBeanServer}引用的{@link FactoryBean}.
 * 公开{@code MBeanServer}以获取bean引用.
 *
 * <p>默认情况下, {@code MBeanServerFactoryBean}将始终创建一个新的{@code MBeanServer}, 即使其中一个已经在运行.
 * 要让{@code MBeanServerFactoryBean}首先尝试查找正在运行的{@code MBeanServer},
 * 将"locateExistingServerIfPossible"属性的值设置为"true".
 */
public class MBeanServerFactoryBean implements FactoryBean<MBeanServer>, InitializingBean, DisposableBean {

	protected final Log logger = LogFactory.getLog(getClass());

	private boolean locateExistingServerIfPossible = false;

	private String agentId;

	private String defaultDomain;

	private boolean registerWithFactory = true;

	private MBeanServer server;

	private boolean newlyRegistered = false;


	/**
	 * 设置{@code MBeanServerFactoryBean}是否应该在创建一个{@code MBeanServer}之前尝试找到它.
	 * <p>默认 {@code false}.
	 */
	public void setLocateExistingServerIfPossible(boolean locateExistingServerIfPossible) {
		this.locateExistingServerIfPossible = locateExistingServerIfPossible;
	}

	/**
	 * 设置{@code MBeanServer}的代理ID以进行定位.
	 * <p>默认无. 如果指定, 这将导致自动尝试定位服务的MBeanServer,
	 * 并且(重要的是) 如果找不到所述MBeanServer, 则不会尝试创建新的MBeanServer (并且将在解析时抛出MBeanServerNotFoundException).
	 * <p>指定空字符串表示平台MBeanServer.
	 */
	public void setAgentId(String agentId) {
		this.agentId = agentId;
	}

	/**
	 * 设置{@code MBeanServer}使用的默认域,
	 * 以传递给{@code MBeanServerFactory.createMBeanServer()}或{@code MBeanServerFactory.findMBeanServer()}.
	 * <p>Default is none.
	 */
	public void setDefaultDomain(String defaultDomain) {
		this.defaultDomain = defaultDomain;
	}

	/**
	 * 设置是否使用{@code MBeanServerFactory}注册{@code MBeanServer},
	 * 通过{@code MBeanServerFactory.findMBeanServer()}使其可用.
	 */
	public void setRegisterWithFactory(boolean registerWithFactory) {
		this.registerWithFactory = registerWithFactory;
	}


	/**
	 * 创建{@code MBeanServer}实例.
	 */
	@Override
	public void afterPropertiesSet() throws MBeanServerNotFoundException {
		// 如果需要, 尝试定位现有的MBeanServer.
		if (this.locateExistingServerIfPossible || this.agentId != null) {
			try {
				this.server = locateMBeanServer(this.agentId);
			}
			catch (MBeanServerNotFoundException ex) {
				// 如果指定了agentId, 只应该定位特定的MBeanServer; so let's bail if we can't find it.
				if (this.agentId != null) {
					throw ex;
				}
				logger.info("No existing MBeanServer found - creating new one");
			}
		}

		// Create a new MBeanServer and register it, if desired.
		if (this.server == null) {
			this.server = createMBeanServer(this.defaultDomain, this.registerWithFactory);
			this.newlyRegistered = this.registerWithFactory;
		}
	}

	/**
	 * 尝试定位现有的{@code MBeanServer}.
	 * 如果{@code locateExistingServerIfPossible}设置为{@code true}, 则调用.
	 * <p>默认实现尝试使用标准查找来查找{@code MBeanServer}. 子类可以覆盖以添加其他位置逻辑.
	 * 
	 * @param agentId 要检索的MBeanServer的代理标识符.
	 * 如果此参数为{@code null}, 则会考虑所有已注册的MBeanServer.
	 * 
	 * @return 如果找到了, 则为{@code MBeanServer}
	 * @throws org.springframework.jmx.MBeanServerNotFoundException 如果找不到{@code MBeanServer}
	 */
	protected MBeanServer locateMBeanServer(String agentId) throws MBeanServerNotFoundException {
		return JmxUtils.locateMBeanServer(agentId);
	}

	/**
	 * 如果需要, 创建一个新的{@code MBeanServer}实例, 并将其注册到{@code MBeanServerFactory}.
	 * 
	 * @param defaultDomain 默认域名, 或{@code null}
	 * @param registerWithFactory 是否使用{@code MBeanServerFactory}注册{@code MBeanServer}
	 */
	protected MBeanServer createMBeanServer(String defaultDomain, boolean registerWithFactory) {
		if (registerWithFactory) {
			return MBeanServerFactory.createMBeanServer(defaultDomain);
		}
		else {
			return MBeanServerFactory.newMBeanServer(defaultDomain);
		}
	}


	@Override
	public MBeanServer getObject() {
		return this.server;
	}

	@Override
	public Class<? extends MBeanServer> getObjectType() {
		return (this.server != null ? this.server.getClass() : MBeanServer.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}


	/**
	 * 注销{@code MBeanServer}实例.
	 */
	@Override
	public void destroy() {
		if (this.newlyRegistered) {
			MBeanServerFactory.releaseMBeanServer(this.server);
		}
	}
}
