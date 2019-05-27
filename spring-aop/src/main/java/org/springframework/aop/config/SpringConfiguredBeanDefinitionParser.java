package org.springframework.aop.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;

/**
 * {@link BeanDefinitionParser}负责解析{@code <aop:spring-configured/>}标签.
 *
 * <p><b>NOTE:</b>对于{@code <context:spring-configured/>}标签，这基本上是Spring 2.5的
 * {@link org.springframework.context.config.SpringConfiguredBeanDefinitionParser}的副本, 
 * 镜像在这里是为了与Spring 2.0的{@code <aop:spring-configured/>}标签兼容 (避免直接依赖上下文包).
 */
class SpringConfiguredBeanDefinitionParser implements BeanDefinitionParser {

	/**
	 * 内部管理的bean配置切面的bean名称.
	 */
	public static final String BEAN_CONFIGURER_ASPECT_BEAN_NAME =
			"org.springframework.context.config.internalBeanConfigurerAspect";

	private static final String BEAN_CONFIGURER_ASPECT_CLASS_NAME =
			"org.springframework.beans.factory.aspectj.AnnotationBeanConfigurerAspect";


	@Override
	public BeanDefinition parse(Element element, ParserContext parserContext) {
		if (!parserContext.getRegistry().containsBeanDefinition(BEAN_CONFIGURER_ASPECT_BEAN_NAME)) {
			RootBeanDefinition def = new RootBeanDefinition();
			def.setBeanClassName(BEAN_CONFIGURER_ASPECT_CLASS_NAME);
			def.setFactoryMethodName("aspectOf");
			def.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			def.setSource(parserContext.extractSource(element));
			parserContext.registerBeanComponent(new BeanComponentDefinition(def, BEAN_CONFIGURER_ASPECT_BEAN_NAME));
		}
		return null;
	}
}
