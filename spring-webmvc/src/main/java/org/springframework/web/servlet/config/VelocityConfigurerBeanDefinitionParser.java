package org.springframework.web.servlet.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSimpleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;


/**
 * Parse the <mvc:velocity-configurer> MVC namespace element and register an
 * VelocityConfigurer bean
 */
public class VelocityConfigurerBeanDefinitionParser extends AbstractSimpleBeanDefinitionParser {

	public static final String BEAN_NAME = "mvcVelocityConfigurer";

	@Override
	protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) {
		return BEAN_NAME;
	}

	@Override
	protected String getBeanClassName(Element element) {
		return "org.springframework.web.servlet.view.velocity.VelocityConfigurer";
	}

	@Override
	protected boolean isEligibleAttribute(String attributeName) {
		return attributeName.equals("resource-loader-path");
	}

	@Override
	protected void postProcess(BeanDefinitionBuilder builder, Element element) {
		if (!builder.getBeanDefinition().hasAttribute("resourceLoaderPath")) {
			builder.getBeanDefinition().setAttribute("resourceLoaderPath", "/WEB-INF/");
		}
	}
}
