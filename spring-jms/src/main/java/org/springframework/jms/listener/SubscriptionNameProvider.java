package org.springframework.jms.listener;

/**
 * 由消息监听器对象实现的接口, 它们建议可能注册的持久订阅的特定名称.
 * 否则, 监听器类名将用作默认订阅名称.
 *
 * <p>适用于{@link javax.jms.MessageListener}对象以及{@link SessionAwareMessageListener}对象和普通监听器方法
 * (由{@link org.springframework.jms.listener.adapter.MessageListenerAdapter}支持.
 */
public interface SubscriptionNameProvider {

	/**
	 * 确定此消息监听器对象的订阅名称.
	 */
	String getSubscriptionName();

}
