package org.springframework.oxm.config;

import java.util.List;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * {@code <oxm:jaxb2-marshaller/>}元素的解析器.
 */
class Jaxb2MarshallerBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

	@Override
	protected String getBeanClassName(Element element) {
		return "org.springframework.oxm.jaxb.Jaxb2Marshaller";
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder beanDefinitionBuilder) {
		String contextPath = element.getAttribute("context-path");
		if (!StringUtils.hasText(contextPath)) {
			// 向后兼容xsd的3.x版本
			contextPath = element.getAttribute("contextPath");
		}
		if (StringUtils.hasText(contextPath)) {
			beanDefinitionBuilder.addPropertyValue("contextPath", contextPath);
		}

		List<Element> classes = DomUtils.getChildElementsByTagName(element, "class-to-be-bound");
		if (!classes.isEmpty()) {
			ManagedList<String> classesToBeBound = new ManagedList<String>(classes.size());
			for (Element classToBeBound : classes) {
				String className = classToBeBound.getAttribute("name");
				classesToBeBound.add(className);
			}
			beanDefinitionBuilder.addPropertyValue("classesToBeBound", classesToBeBound);
		}
	}

}
