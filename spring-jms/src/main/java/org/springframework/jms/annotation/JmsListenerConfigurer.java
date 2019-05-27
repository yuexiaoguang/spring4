package org.springframework.jms.annotation;

import org.springframework.jms.config.JmsListenerEndpointRegistrar;

/**
 * 可由Spring托管bean实现的可选接口, 该bean愿意自定义如何配置JMS监听器端点.
 * 通常用于定义默认的{@link org.springframework.jms.config.JmsListenerContainerFactory JmsListenerContainerFactory},
 * 或以<em>编程</em>方式注册JMS端点, 而不是使用@{@link JmsListener}注解的<em>声明性</em>方法.
 *
 * <p>See @{@link EnableJms} for detailed usage examples.
 */
public interface JmsListenerConfigurer {

	/**
	 * 回调允许针对给定的{@link JmsListenerEndpointRegistrar}注册
	 * {@link org.springframework.jms.config.JmsListenerEndpointRegistry JmsListenerEndpointRegistry}
	 * 和特定的{@link org.springframework.jms.config.JmsListenerEndpoint JmsListenerEndpoint}实例.
	 * 默认的{@link org.springframework.jms.config.JmsListenerContainerFactory JmsListenerContainerFactory}也可以定制.
	 * 
	 * @param registrar 要配置的注册商
	 */
	void configureJmsListeners(JmsListenerEndpointRegistrar registrar);

}
