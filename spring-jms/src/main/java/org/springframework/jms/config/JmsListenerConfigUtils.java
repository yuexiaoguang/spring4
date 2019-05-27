package org.springframework.jms.config;

/**
 * 跨子包进行内部共享的配置常量.
 */
public abstract class JmsListenerConfigUtils {

	/**
	 * 内部管理的JMS监听器注解处理器的bean名称.
	 */
	public static final String JMS_LISTENER_ANNOTATION_PROCESSOR_BEAN_NAME =
			"org.springframework.jms.config.internalJmsListenerAnnotationProcessor";

	/**
	 * 内部管理的JMS监听器端点注册表的bean名称.
	 */
	public static final String JMS_LISTENER_ENDPOINT_REGISTRY_BEAN_NAME =
			"org.springframework.jms.config.internalJmsListenerEndpointRegistry";

}
