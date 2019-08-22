package org.springframework.web.socket.config;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import org.springframework.util.ClassUtils;

/**
 * {@link org.springframework.beans.factory.xml.NamespaceHandler}, 用于Spring WebSocket配置命名空间.
 */
public class WebSocketNamespaceHandler extends NamespaceHandlerSupport {

	private static boolean isSpringMessagingPresent = ClassUtils.isPresent(
			"org.springframework.messaging.Message", WebSocketNamespaceHandler.class.getClassLoader());


	@Override
	public void init() {
		registerBeanDefinitionParser("handlers", new HandlersBeanDefinitionParser());
		if (isSpringMessagingPresent) {
			registerBeanDefinitionParser("message-broker", new MessageBrokerBeanDefinitionParser());
		}
	}
}
