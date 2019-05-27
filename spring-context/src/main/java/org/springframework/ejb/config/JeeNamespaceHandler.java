package org.springframework.ejb.config;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * '{@code jee}'命名空间的{@link org.springframework.beans.factory.xml.NamespaceHandler}.
 */
public class JeeNamespaceHandler extends NamespaceHandlerSupport {

	@Override
	public void init() {
		registerBeanDefinitionParser("jndi-lookup", new JndiLookupBeanDefinitionParser());
		registerBeanDefinitionParser("local-slsb", new LocalStatelessSessionBeanDefinitionParser());
		registerBeanDefinitionParser("remote-slsb", new RemoteStatelessSessionBeanDefinitionParser());
	}

}
