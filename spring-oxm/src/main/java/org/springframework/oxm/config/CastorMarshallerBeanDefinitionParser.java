package org.springframework.oxm.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.xml.AbstractSimpleBeanDefinitionParser;

/**
 * Parser for the {@code <oxm:castor-marshaller/>} element.
 *
 * @deprecated as of Spring Framework 4.3.13, due to the lack of activity on the Castor project
 */
@Deprecated
class CastorMarshallerBeanDefinitionParser extends AbstractSimpleBeanDefinitionParser {

	@Override
	protected String getBeanClassName(Element element) {
		return "org.springframework.oxm.castor.CastorMarshaller";
	}

}
