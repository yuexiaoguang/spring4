package org.springframework.oxm.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.xml.AbstractSimpleBeanDefinitionParser;

/**
 * Parser for the {@code <oxm:jibx-marshaller/>} element.
 */
class JibxMarshallerBeanDefinitionParser extends AbstractSimpleBeanDefinitionParser {

	@Override
	protected String getBeanClassName(Element element) {
		return "org.springframework.oxm.jibx.JibxMarshaller";
	}

}
