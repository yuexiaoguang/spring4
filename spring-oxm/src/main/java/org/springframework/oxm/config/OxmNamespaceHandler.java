package org.springframework.oxm.config;

import org.springframework.beans.factory.xml.NamespaceHandler;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * '{@code oxm}'命名空间的{@link NamespaceHandler}.
 */
public class OxmNamespaceHandler extends NamespaceHandlerSupport {

	@Override
	@SuppressWarnings("deprecation")
	public void init() {
		registerBeanDefinitionParser("jaxb2-marshaller", new Jaxb2MarshallerBeanDefinitionParser());
		registerBeanDefinitionParser("jibx-marshaller", new JibxMarshallerBeanDefinitionParser());
		registerBeanDefinitionParser("castor-marshaller", new CastorMarshallerBeanDefinitionParser());
		registerBeanDefinitionParser("xmlbeans-marshaller", new XmlBeansMarshallerBeanDefinitionParser());
	}

}
