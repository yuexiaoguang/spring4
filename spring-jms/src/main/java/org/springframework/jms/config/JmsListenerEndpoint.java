package org.springframework.jms.config;

import org.springframework.jms.listener.MessageListenerContainer;

/**
 * JMS监听器端点的模型.
 * 可用于{@link org.springframework.jms.annotation.JmsListenerConfigurer JmsListenerConfigurer}以编程方式注册端点.
 */
public interface JmsListenerEndpoint {

	/**
	 * 返回此端点的id.
	 */
	String getId();

	/**
	 * 使用此端点定义的模型, 设置指定的消息监听器容器.
	 * <p>此端点必须提供指定容器请求的缺失选项以使其可用.
	 * 通常, 这是关于设置要使用的{@code destination}和{@code messageListener}, 但实现可能会覆盖已设置的任何默认设置.
	 * 
	 * @param listenerContainer 要配置的监听器容器
	 */
	void setupListenerContainer(MessageListenerContainer listenerContainer);

}
