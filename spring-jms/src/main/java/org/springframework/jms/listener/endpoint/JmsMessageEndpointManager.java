package org.springframework.jms.listener.endpoint;

import javax.jms.MessageListener;
import javax.resource.ResourceException;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.jca.endpoint.GenericMessageEndpointManager;
import org.springframework.jms.listener.MessageListenerContainer;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.destination.DestinationResolver;

/**
 * 通用JCA 1.5 {@link org.springframework.jca.endpoint.GenericMessageEndpointManager}的扩展,
 * 为ActivationSpec配置添加特定于JMS的支持.
 *
 * <p>允许定义一个公共的{@link JmsActivationSpecConfig}对象,
 * 该对象被转换为特定于提供者的JCA 1.5 ActivationSpec对象, 用于激活端点.
 *
 * <p><b>NOTE:</b> 此基于JCA的端点管理器仅支持标准JMS {@link javax.jms.MessageListener}端点.
 * <i>不</i>支持Spring的{@link org.springframework.jms.listener.SessionAwareMessageListener}变体,
 * 因为JCA端点管理约定不允许获取当前的JMS {@link javax.jms.Session}.
 */
public class JmsMessageEndpointManager extends GenericMessageEndpointManager
		implements BeanNameAware, MessageListenerContainer {

	private final JmsMessageEndpointFactory endpointFactory = new JmsMessageEndpointFactory();

	private boolean messageListenerSet = false;

	private JmsActivationSpecFactory activationSpecFactory = new DefaultJmsActivationSpecFactory();

	private JmsActivationSpecConfig activationSpecConfig;


	/**
	 * 设置此端点的JMS MessageListener.
	 * <p>这是配置专用JmsMessageEndpointFactory的快捷方式.
	 */
	public void setMessageListener(MessageListener messageListener) {
		this.endpointFactory.setMessageListener(messageListener);
		this.messageListenerSet = true;
	}

	/**
	 * 返回此端点的JMS MessageListener.
	 */
	public MessageListener getMessageListener() {
		return this.endpointFactory.getMessageListener();
	}

	/**
	 * 设置XA 事务管理器, 用于包装端点调用, 在每个此类事务中登记端点资源.
	 * <p>传入的对象可以是实现Spring的{@link org.springframework.transaction.jta.TransactionFactory}接口的事务管理器,
	 * 也可以是普通的{@link javax.transaction.TransactionManager}.
	 * <p>如果未指定事务管理器, 则端点调用将不会包装在XA事务中.
	 * 有关特定提供商的本地事务选项, 请参阅资源提供商的ActivationSpec文档.
	 * <p>这是配置专用JmsMessageEndpointFactory的快捷方式.
	 */
	public void setTransactionManager(Object transactionManager) {
		this.endpointFactory.setTransactionManager(transactionManager);
	}

	/**
	 * 为具体的JCA 1.5 ActivationSpec对象设置工厂,
	 * 根据{@link #setActivationSpecConfig JmsActivationSpecConfig}对象创建JCA ActivationSpec.
	 * <p>该工厂依赖于具体的JMS提供者, e.g. on ActiveMQ.
	 * 默认实现只是从提供者的类名中猜测ActivationSpec类名 (e.g. "ActiveMQResourceAdapter" -> "ActiveMQActivationSpec"在同一个包中),
	 * 并按照JCA 1.5规范 (以及一些自动检测的特定于供应商的属性)的建议填充ActivationSpec属性.
	 */
	public void setActivationSpecFactory(JmsActivationSpecFactory activationSpecFactory) {
		this.activationSpecFactory =
				(activationSpecFactory != null ? activationSpecFactory : new DefaultJmsActivationSpecFactory());
	}

	/**
	 * 设置DestinationResolver, 用于将目标名称解析为JCA 1.5 ActivationSpec "destination"属性.
	 * <p>如果未指定, 则目标名称将仅作为字符串传递.
	 * 如果指定, 则首先将目标名称解析为Destination对象.
	 * <p>请注意, DestinationResolver通常在JmsActivationSpecFactory上指定
	 * (see {@link StandardJmsActivationSpecFactory#setDestinationResolver}).
	 * 这只是参数化默认JmsActivationSpecFactory的快捷方式; 它将替换之前可能已设置的任何自定义JmsActivationSpecFactory.
	 */
	public void setDestinationResolver(DestinationResolver destinationResolver) {
		DefaultJmsActivationSpecFactory factory = new DefaultJmsActivationSpecFactory();
		factory.setDestinationResolver(destinationResolver);
		this.activationSpecFactory = factory;
	}

	/**
	 * 指定此端点管理器用于激活其监听器的{@link JmsActivationSpecConfig}对象.
	 * <p>此配置对象将通过{@link #setActivationSpecFactory JmsActivationSpecFactory}转换为具体的JCA 1.5 ActivationSpec对象.
	 */
	public void setActivationSpecConfig(JmsActivationSpecConfig activationSpecConfig) {
		this.activationSpecConfig = activationSpecConfig;
	}

	/**
	 * 返回此端点管理器用于激活其监听器的{@link JmsActivationSpecConfig}对象.
	 * 如果没有设置, 则返回{@code null}.
	 */
	public JmsActivationSpecConfig getActivationSpecConfig() {
		return this.activationSpecConfig;
	}

	/**
	 * 设置此消息端点的名称.
	 * 在Spring的bean工厂中定义时自动填充的bean名称.
	 */
	@Override
	public void setBeanName(String beanName) {
		this.endpointFactory.setBeanName(beanName);
	}


	@Override
	public void afterPropertiesSet() throws ResourceException {
		if (this.messageListenerSet) {
			setMessageEndpointFactory(this.endpointFactory);
		}
		if (this.activationSpecConfig != null) {
			setActivationSpec(
					this.activationSpecFactory.createActivationSpec(getResourceAdapter(), this.activationSpecConfig));
		}
		super.afterPropertiesSet();
	}


	@Override
	public void setupMessageListener(Object messageListener) {
		if (messageListener instanceof MessageListener) {
			setMessageListener((MessageListener) messageListener);
		}
		else {
			throw new IllegalArgumentException("Unsupported message listener '" +
					messageListener.getClass().getName() + "': only '" + MessageListener.class.getName() +
					"' type is supported");
		}
	}

	@Override
	public MessageConverter getMessageConverter() {
		JmsActivationSpecConfig config = getActivationSpecConfig();
		if (config != null) {
			return config.getMessageConverter();
		}
		return null;
	}

	@Override
	public DestinationResolver getDestinationResolver() {
		if (this.activationSpecFactory instanceof StandardJmsActivationSpecFactory) {
			return ((StandardJmsActivationSpecFactory) this.activationSpecFactory).getDestinationResolver();
		}
		return null;
	}

	@Override
	public boolean isPubSubDomain() {
		JmsActivationSpecConfig config = getActivationSpecConfig();
		if (config != null) {
			return config.isPubSubDomain();
		}
		throw new IllegalStateException("Could not determine pubSubDomain - no activation spec config is set");
	}

	@Override
	public boolean isReplyPubSubDomain() {
		JmsActivationSpecConfig config = getActivationSpecConfig();
		if (config != null) {
			return config.isReplyPubSubDomain();
		}
		throw new IllegalStateException("Could not determine reply pubSubDomain - no activation spec config is set");
	}

}
