package org.springframework.oxm.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;

/**
 * {@code <oxm:xmlbeans-marshaller/>}元素的解析器.
 *
 * @deprecated as of Spring 4.2, following the XMLBeans retirement at Apache
 */
@Deprecated
class XmlBeansMarshallerBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

	@Override
	protected String getBeanClassName(Element element) {
		return "org.springframework.oxm.xmlbeans.XmlBeansMarshaller";
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder beanDefinitionBuilder) {
		String optionsName = element.getAttribute("options");
		if (StringUtils.hasText(optionsName)) {
			beanDefinitionBuilder.addPropertyReference("xmlOptions", optionsName);
		}
	}

}
