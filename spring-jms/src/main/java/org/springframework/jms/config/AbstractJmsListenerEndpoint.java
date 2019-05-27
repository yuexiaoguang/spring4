package org.springframework.jms.config;

import javax.jms.MessageListener;

import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.jms.listener.MessageListenerContainer;
import org.springframework.jms.listener.endpoint.JmsActivationSpecConfig;
import org.springframework.jms.listener.endpoint.JmsMessageEndpointManager;

/**
 * JMS监听器端点的基本模型
 */
public abstract class AbstractJmsListenerEndpoint implements JmsListenerEndpoint {

	private String id;

	private String destination;

	private String subscription;

	private String selector;

	private String concurrency;


	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String getId() {
		return this.id;
	}

	/**
	 * 设置此端点的目标名称.
	 */
	public void setDestination(String destination) {
		this.destination = destination;
	}

	/**
	 * 返回此端点的目标名称.
	 */
	public String getDestination() {
		return this.destination;
	}

	/**
	 * 设置持久订阅的名称.
	 */
	public void setSubscription(String subscription) {
		this.subscription = subscription;
	}

	/**
	 * 返回持久订阅的名称.
	 */
	public String getSubscription() {
		return this.subscription;
	}

	/**
	 * 设置JMS消息选择器表达式.
	 * <p>有关选择器表达式的详细定义, 请参阅JMS规范.
	 */
	public void setSelector(String selector) {
		this.selector = selector;
	}

	/**
	 * 返回JMS消息选择器表达式.
	 */
	public String getSelector() {
		return this.selector;
	}

	/**
	 * 设置监听器的并发限制.
	 * <p>并发限制可以是"下限-上限" String, e.g. "5-10", 或简单的上限字符串, e.g. "10" (在这种情况下, 下限为1).
	 * <p>底层容器可能支持也可能不支持所有功能. 例如, 它可能无法缩放: 在这种情况下, 仅使用上限值.
	 */
	public void setConcurrency(String concurrency) {
		this.concurrency = concurrency;
	}

	/**
	 * 返回监听器的并发限制.
	 */
	public String getConcurrency() {
		return this.concurrency;
	}


	@Override
	public void setupListenerContainer(MessageListenerContainer listenerContainer) {
		if (listenerContainer instanceof AbstractMessageListenerContainer) {
			setupJmsListenerContainer((AbstractMessageListenerContainer) listenerContainer);
		}
		else {
			new JcaEndpointConfigurer().configureEndpoint(listenerContainer);
		}
	}

	private void setupJmsListenerContainer(AbstractMessageListenerContainer listenerContainer) {
		if (getDestination() != null) {
			listenerContainer.setDestinationName(getDestination());
		}
		if (getSubscription() != null) {
			listenerContainer.setSubscriptionName(getSubscription());
		}
		if (getSelector() != null) {
			listenerContainer.setMessageSelector(getSelector());
		}
		if (getConcurrency() != null) {
			listenerContainer.setConcurrency(getConcurrency());
		}
		setupMessageListener(listenerContainer);
	}

	/**
	 * 创建一个{@link MessageListener}, 它能够为指定容器提供此端点服务.
	 */
	protected abstract MessageListener createMessageListener(MessageListenerContainer container);

	private void setupMessageListener(MessageListenerContainer container) {
		MessageListener messageListener = createMessageListener(container);
		if (messageListener == null) {
			throw new IllegalStateException("Endpoint [" + this + "] must provide a non-null message listener");
		}
		container.setupMessageListener(messageListener);
	}

	/**
	 * 返回此端点的描述.
	 * <p>可用于子类, 包含在其{@code toString()}结果中.
	 */
	protected StringBuilder getEndpointDescription() {
		StringBuilder result = new StringBuilder();
		return result.append(getClass().getSimpleName()).append("[").append(this.id).append("] destination=").
				append(this.destination).append("' | subscription='").append(this.subscription).
				append(" | selector='").append(this.selector).append("'");
	}

	@Override
	public String toString() {
		return getEndpointDescription().toString();
	}


	/**
	 * 内部类, 以避免对JCA API的硬依赖.
	 */
	private class JcaEndpointConfigurer {

		public void configureEndpoint(Object listenerContainer) {
			if (listenerContainer instanceof JmsMessageEndpointManager) {
				setupJcaMessageContainer((JmsMessageEndpointManager) listenerContainer);
			}
			else {
				throw new IllegalArgumentException("Could not configure endpoint with the specified container '" +
						listenerContainer + "' Only JMS (" + AbstractMessageListenerContainer.class.getName() +
						" subclass) or JCA (" + JmsMessageEndpointManager.class.getName() + ") are supported.");
			}
		}

		private void setupJcaMessageContainer(JmsMessageEndpointManager container) {
			JmsActivationSpecConfig activationSpecConfig = container.getActivationSpecConfig();
			if (activationSpecConfig == null) {
				activationSpecConfig = new JmsActivationSpecConfig();
				container.setActivationSpecConfig(activationSpecConfig);
			}
			if (getDestination() != null) {
				activationSpecConfig.setDestinationName(getDestination());
			}
			if (getSubscription() != null) {
				activationSpecConfig.setSubscriptionName(getSubscription());
			}
			if (getSelector() != null) {
				activationSpecConfig.setMessageSelector(getSelector());
			}
			if (getConcurrency() != null) {
				activationSpecConfig.setConcurrency(getConcurrency());
			}
			setupMessageListener(container);
		}
	}

}
