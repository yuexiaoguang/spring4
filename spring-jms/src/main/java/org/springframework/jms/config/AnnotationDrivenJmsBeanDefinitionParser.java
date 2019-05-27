package org.springframework.jms.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.parsing.CompositeComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;

/**
 * 'jms'命名空间的'注解驱动'元素的解析器.
 */
class AnnotationDrivenJmsBeanDefinitionParser implements BeanDefinitionParser {

	@Override
	public BeanDefinition parse(Element element, ParserContext parserContext) {
		Object source = parserContext.extractSource(element);

		// 注册环绕<jms:annotation-driven>元素的组件.
		CompositeComponentDefinition compDefinition = new CompositeComponentDefinition(element.getTagName(), source);
		parserContext.pushContainingComponent(compDefinition);

		// 将具体的后处理器bean嵌入周围的组件中.
		BeanDefinitionRegistry registry = parserContext.getRegistry();

		if (registry.containsBeanDefinition(JmsListenerConfigUtils.JMS_LISTENER_ANNOTATION_PROCESSOR_BEAN_NAME)) {
			parserContext.getReaderContext().error(
					"Only one JmsListenerAnnotationBeanPostProcessor may exist within the context.", source);
		}
		else {
			BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(
					"org.springframework.jms.annotation.JmsListenerAnnotationBeanPostProcessor");
			builder.getRawBeanDefinition().setSource(source);
			String endpointRegistry = element.getAttribute("registry");
			if (StringUtils.hasText(endpointRegistry)) {
				builder.addPropertyReference("endpointRegistry", endpointRegistry);
			}
			else {
				registerDefaultEndpointRegistry(source, parserContext);
			}

			String containerFactory = element.getAttribute("container-factory");
			if (StringUtils.hasText(containerFactory)) {
				builder.addPropertyValue("containerFactoryBeanName", containerFactory);
			}

			String handlerMethodFactory = element.getAttribute("handler-method-factory");
			if (StringUtils.hasText(handlerMethodFactory)) {
				builder.addPropertyReference("messageHandlerMethodFactory", handlerMethodFactory);
			}

			registerInfrastructureBean(parserContext, builder,
					JmsListenerConfigUtils.JMS_LISTENER_ANNOTATION_PROCESSOR_BEAN_NAME);
		}

		// 最后注册复合组件.
		parserContext.popAndRegisterContainingComponent();

		return null;
	}

	private static void registerDefaultEndpointRegistry(Object source, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(
				"org.springframework.jms.config.JmsListenerEndpointRegistry");
		builder.getRawBeanDefinition().setSource(source);
		registerInfrastructureBean(parserContext, builder, JmsListenerConfigUtils.JMS_LISTENER_ENDPOINT_REGISTRY_BEAN_NAME);
	}

	private static void registerInfrastructureBean(
			ParserContext parserContext, BeanDefinitionBuilder builder, String beanName) {

		builder.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		parserContext.getRegistry().registerBeanDefinition(beanName, builder.getBeanDefinition());
		BeanDefinitionHolder holder = new BeanDefinitionHolder(builder.getBeanDefinition(), beanName);
		parserContext.registerComponent(new BeanComponentDefinition(holder));
	}

}
