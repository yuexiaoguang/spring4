package org.springframework.jms.config;

import org.springframework.jms.listener.SimpleMessageListenerContainer;

/**
 * 用于构建标准{@link SimpleMessageListenerContainer}的{@link JmsListenerContainerFactory}实现.
 */
public class SimpleJmsListenerContainerFactory
		extends AbstractJmsListenerContainerFactory<SimpleMessageListenerContainer> {

	@Override
	protected SimpleMessageListenerContainer createContainerInstance() {
		return new SimpleMessageListenerContainer();
	}

}
