package org.springframework.jms.config;

import org.springframework.jms.listener.MessageListenerContainer;

/**
 * 基于{@link JmsListenerEndpoint}定义的{@link MessageListenerContainer}工厂.
 */
public interface JmsListenerContainerFactory<C extends MessageListenerContainer> {

	/**
	 * 为给定的{@link JmsListenerEndpoint}创建一个{@link MessageListenerContainer}.
	 * 
	 * @param endpoint 要配置的端点
	 * 
	 * @return 创建的容器
	 */
	C createListenerContainer(JmsListenerEndpoint endpoint);

}
