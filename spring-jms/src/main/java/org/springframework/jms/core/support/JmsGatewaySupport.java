package org.springframework.jms.core.support;

import javax.jms.ConnectionFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jms.core.JmsTemplate;

/**
 * 用于需要JMS访问的应用程序类的超类.
 *
 * <p>需要设置ConnectionFactory或JmsTemplate实例.
 * 如果传入ConnectionFactory, 它将创建自己的JmsTemplate.
 * 可以通过覆盖{@link #createJmsTemplate}方法为给定的ConnectionFactory创建自定义JmsTemplate实例.
 */
public abstract class JmsGatewaySupport implements InitializingBean {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private JmsTemplate jmsTemplate;


	/**
	 * 设置网关使用的JMS连接工厂.
	 * 将自动为给定的ConnectionFactory创建JmsTemplate.
	 */
	public final void setConnectionFactory(ConnectionFactory connectionFactory) {
		this.jmsTemplate = createJmsTemplate(connectionFactory);
	}

	/**
	 * 为给定的ConnectionFactory创建一个JmsTemplate.
	 * 仅在使用ConnectionFactory引用填充网关时调用.
	 * <p>可以在子类中重写以提供具有不同配置的JmsTemplate实例.
	 * 
	 * @param connectionFactory 用于创建JmsTemplate的JMS ConnectionFactory
	 * 
	 * @return 新的JmsTemplate 实例
	 */
	protected JmsTemplate createJmsTemplate(ConnectionFactory connectionFactory) {
		return new JmsTemplate(connectionFactory);
	}

	/**
	 * 返回网关使用的JMS ConnectionFactory.
	 */
	public final ConnectionFactory getConnectionFactory() {
		return (this.jmsTemplate != null ? this.jmsTemplate.getConnectionFactory() : null);
	}

	/**
	 * 设置网关的JmsTemplate.
	 */
	public final void setJmsTemplate(JmsTemplate jmsTemplate) {
		this.jmsTemplate = jmsTemplate;
	}

	/**
	 * 返回网关的JmsTemplate.
	 */
	public final JmsTemplate getJmsTemplate() {
		return this.jmsTemplate;
	}

	@Override
	public final void afterPropertiesSet() throws IllegalArgumentException, BeanInitializationException {
		if (this.jmsTemplate == null) {
			throw new IllegalArgumentException("'connectionFactory' or 'jmsTemplate' is required");
		}
		try {
			initGateway();
		}
		catch (Exception ex) {
			throw new BeanInitializationException("Initialization of JMS gateway failed: " + ex.getMessage(), ex);
		}
	}

	/**
	 * 子类可以覆盖此自定义初始化行为.
	 * 在此实例的bean属性的填充之后调用.
	 * 
	 * @throws java.lang.Exception 如果初始化失败
	 */
	protected void initGateway() throws Exception {
	}

}
