package org.springframework.ejb.config;

import org.w3c.dom.Element;

import org.springframework.ejb.access.LocalStatelessSessionProxyFactoryBean;

/**
 * {@link org.springframework.beans.factory.xml.BeanDefinitionParser}实现,
 * 用于解析'{@code local-slsb}'标签, 并创建 {@link LocalStatelessSessionProxyFactoryBean}定义.
 */
class LocalStatelessSessionBeanDefinitionParser extends AbstractJndiLocatingBeanDefinitionParser {

	@Override
	protected String getBeanClassName(Element element) {
		return "org.springframework.ejb.access.LocalStatelessSessionProxyFactoryBean";
	}

}
