package org.springframework.scheduling.config;

import org.w3c.dom.Element;

import org.springframework.aop.config.AopNamespaceUtils;
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
 * 解析'task'命名空间的'annotation-driven'元素.
 */
public class AnnotationDrivenBeanDefinitionParser implements BeanDefinitionParser {

	private static final String ASYNC_EXECUTION_ASPECT_CLASS_NAME =
			"org.springframework.scheduling.aspectj.AnnotationAsyncExecutionAspect";


	@Override
	public BeanDefinition parse(Element element, ParserContext parserContext) {
		Object source = parserContext.extractSource(element);

		// 注册环绕<task:annotation-driven> element元素的组件.
		CompositeComponentDefinition compDefinition = new CompositeComponentDefinition(element.getTagName(), source);
		parserContext.pushContainingComponent(compDefinition);

		// 将具体的后处理器bean嵌入环绕的组件中.
		BeanDefinitionRegistry registry = parserContext.getRegistry();

		String mode = element.getAttribute("mode");
		if ("aspectj".equals(mode)) {
			// mode="aspectj"
			registerAsyncExecutionAspect(element, parserContext);
		}
		else {
			// mode="proxy"
			if (registry.containsBeanDefinition(TaskManagementConfigUtils.ASYNC_ANNOTATION_PROCESSOR_BEAN_NAME)) {
				parserContext.getReaderContext().error(
						"Only one AsyncAnnotationBeanPostProcessor may exist within the context.", source);
			}
			else {
				BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(
						"org.springframework.scheduling.annotation.AsyncAnnotationBeanPostProcessor");
				builder.getRawBeanDefinition().setSource(source);
				String executor = element.getAttribute("executor");
				if (StringUtils.hasText(executor)) {
					builder.addPropertyReference("executor", executor);
				}
				String exceptionHandler = element.getAttribute("exception-handler");
				if (StringUtils.hasText(exceptionHandler)) {
					builder.addPropertyReference("exceptionHandler", exceptionHandler);
				}
				if (Boolean.valueOf(element.getAttribute(AopNamespaceUtils.PROXY_TARGET_CLASS_ATTRIBUTE))) {
					builder.addPropertyValue("proxyTargetClass", true);
				}
				registerPostProcessor(parserContext, builder, TaskManagementConfigUtils.ASYNC_ANNOTATION_PROCESSOR_BEAN_NAME);
			}
		}

		if (registry.containsBeanDefinition(TaskManagementConfigUtils.SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME)) {
			parserContext.getReaderContext().error(
					"Only one ScheduledAnnotationBeanPostProcessor may exist within the context.", source);
		}
		else {
			BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(
					"org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor");
			builder.getRawBeanDefinition().setSource(source);
			String scheduler = element.getAttribute("scheduler");
			if (StringUtils.hasText(scheduler)) {
				builder.addPropertyReference("scheduler", scheduler);
			}
			registerPostProcessor(parserContext, builder, TaskManagementConfigUtils.SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME);
		}

		// 最后注册复合组件.
		parserContext.popAndRegisterContainingComponent();

		return null;
	}

	private void registerAsyncExecutionAspect(Element element, ParserContext parserContext) {
		if (!parserContext.getRegistry().containsBeanDefinition(TaskManagementConfigUtils.ASYNC_EXECUTION_ASPECT_BEAN_NAME)) {
			BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ASYNC_EXECUTION_ASPECT_CLASS_NAME);
			builder.setFactoryMethod("aspectOf");
			String executor = element.getAttribute("executor");
			if (StringUtils.hasText(executor)) {
				builder.addPropertyReference("executor", executor);
			}
			String exceptionHandler = element.getAttribute("exception-handler");
			if (StringUtils.hasText(exceptionHandler)) {
				builder.addPropertyReference("exceptionHandler", exceptionHandler);
			}
			parserContext.registerBeanComponent(new BeanComponentDefinition(builder.getBeanDefinition(),
					TaskManagementConfigUtils.ASYNC_EXECUTION_ASPECT_BEAN_NAME));
		}
	}

	private static void registerPostProcessor(
			ParserContext parserContext, BeanDefinitionBuilder builder, String beanName) {

		builder.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		parserContext.getRegistry().registerBeanDefinition(beanName, builder.getBeanDefinition());
		BeanDefinitionHolder holder = new BeanDefinitionHolder(builder.getBeanDefinition(), beanName);
		parserContext.registerComponent(new BeanComponentDefinition(holder));
	}

}
