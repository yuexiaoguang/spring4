package org.springframework.web.servlet.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.AbstractSimpleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;

/**
 * 解析<mvc:groovy-configurer> MVC命名空间元素并注册GroovyConfigurer bean
 */
public class GroovyMarkupConfigurerBeanDefinitionParser extends AbstractSimpleBeanDefinitionParser {

	public static final String BEAN_NAME = "mvcGroovyMarkupConfigurer";


	@Override
	protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) {
		return BEAN_NAME;
	}

	@Override
	protected String getBeanClassName(Element element) {
		return "org.springframework.web.servlet.view.groovy.GroovyMarkupConfigurer";
	}

	@Override
	protected boolean isEligibleAttribute(String name) {
		return (name.equals("auto-indent") || name.equals("cache-templates") || name.equals("resource-loader-path"));
	}

}
