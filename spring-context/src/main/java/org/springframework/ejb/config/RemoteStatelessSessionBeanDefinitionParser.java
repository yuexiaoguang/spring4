package org.springframework.ejb.config;

import org.w3c.dom.Element;

import org.springframework.ejb.access.SimpleRemoteStatelessSessionProxyFactoryBean;

/**
 * {@link org.springframework.beans.factory.xml.BeanDefinitionParser}实现,
 * 用于解析'{@code remote-slsb}'标签并创建{@link SimpleRemoteStatelessSessionProxyFactoryBean}定义.
 */
class RemoteStatelessSessionBeanDefinitionParser extends AbstractJndiLocatingBeanDefinitionParser {

	@Override
	protected String getBeanClassName(Element element) {
		return "org.springframework.ejb.access.SimpleRemoteStatelessSessionProxyFactoryBean";
	}

}
