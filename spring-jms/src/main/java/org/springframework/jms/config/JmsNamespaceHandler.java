package org.springframework.jms.config;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * 用于JMS名称空间的{@link org.springframework.beans.factory.xml.NamespaceHandler}.
 */
public class JmsNamespaceHandler extends NamespaceHandlerSupport {

	@Override
	public void init() {
		registerBeanDefinitionParser("listener-container", new JmsListenerContainerParser());
		registerBeanDefinitionParser("jca-listener-container", new JcaListenerContainerParser());
		registerBeanDefinitionParser("annotation-driven", new AnnotationDrivenJmsBeanDefinitionParser());
	}

}
