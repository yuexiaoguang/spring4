package org.springframework.context.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.PropertyOverrideConfigurer;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;

/**
 * &lt;context:property-override/&gt; 元素的解析器.
 */
class PropertyOverrideBeanDefinitionParser extends AbstractPropertyLoadingBeanDefinitionParser {

	@Override
	protected Class<?> getBeanClass(Element element) {
		return PropertyOverrideConfigurer.class;
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		super.doParse(element, parserContext, builder);

		builder.addPropertyValue("ignoreInvalidKeys",
				Boolean.valueOf(element.getAttribute("ignore-unresolvable")));

	}

}
