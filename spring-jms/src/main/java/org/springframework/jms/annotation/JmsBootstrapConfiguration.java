package org.springframework.jms.annotation;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.jms.config.JmsListenerConfigUtils;
import org.springframework.jms.config.JmsListenerEndpointRegistry;

/**
 * {@code @Configuration}类,
 * 注册能够处理Spring的@{@link JmsListener}注解的{@link JmsListenerAnnotationBeanPostProcessor} bean.
 * 也注册默认{@link JmsListenerEndpointRegistry}.
 *
 * <p>使用@{@link EnableJms}注解时, 将自动导入此配置类.
 * 有关完整的使用详细信息, 请参阅{@link EnableJms} javadocs.
 */
@Configuration
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class JmsBootstrapConfiguration {

	@Bean(name = JmsListenerConfigUtils.JMS_LISTENER_ANNOTATION_PROCESSOR_BEAN_NAME)
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public JmsListenerAnnotationBeanPostProcessor jmsListenerAnnotationProcessor() {
		return new JmsListenerAnnotationBeanPostProcessor();
	}

	@Bean(name = JmsListenerConfigUtils.JMS_LISTENER_ENDPOINT_REGISTRY_BEAN_NAME)
	public JmsListenerEndpointRegistry defaultJmsListenerEndpointRegistry() {
		return new JmsListenerEndpointRegistry();
	}

}
