package org.springframework.web.servlet.config;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.springframework.web.cors.CorsConfiguration;

/**
 * {@link org.springframework.beans.factory.xml.BeanDefinitionParser}
 * 解析{@code cors}元素, 以便在{@link AnnotationDrivenBeanDefinitionParser},
 * {@link ResourcesBeanDefinitionParser}和{@link ViewControllerBeanDefinitionParser}
 * 创建的各种{AbstractHandlerMapping} bean中设置CORS配置.
 */
public class CorsBeanDefinitionParser implements BeanDefinitionParser {

	@Override
	public BeanDefinition parse(Element element, ParserContext parserContext) {

		Map<String, CorsConfiguration> corsConfigurations = new LinkedHashMap<String, CorsConfiguration>();
		List<Element> mappings = DomUtils.getChildElementsByTagName(element, "mapping");

		if (mappings.isEmpty()) {
			CorsConfiguration config = new CorsConfiguration().applyPermitDefaultValues();
			corsConfigurations.put("/**", config);
		}
		else {
			for (Element mapping : mappings) {
				CorsConfiguration config = new CorsConfiguration();
				if (mapping.hasAttribute("allowed-origins")) {
					String[] allowedOrigins = StringUtils.tokenizeToStringArray(mapping.getAttribute("allowed-origins"), ",");
					config.setAllowedOrigins(Arrays.asList(allowedOrigins));
				}
				if (mapping.hasAttribute("allowed-methods")) {
					String[] allowedMethods = StringUtils.tokenizeToStringArray(mapping.getAttribute("allowed-methods"), ",");
					config.setAllowedMethods(Arrays.asList(allowedMethods));
				}
				if (mapping.hasAttribute("allowed-headers")) {
					String[] allowedHeaders = StringUtils.tokenizeToStringArray(mapping.getAttribute("allowed-headers"), ",");
					config.setAllowedHeaders(Arrays.asList(allowedHeaders));
				}
				if (mapping.hasAttribute("exposed-headers")) {
					String[] exposedHeaders = StringUtils.tokenizeToStringArray(mapping.getAttribute("exposed-headers"), ",");
					config.setExposedHeaders(Arrays.asList(exposedHeaders));
				}
				if (mapping.hasAttribute("allow-credentials")) {
					config.setAllowCredentials(Boolean.parseBoolean(mapping.getAttribute("allow-credentials")));
				}
				if (mapping.hasAttribute("max-age")) {
					config.setMaxAge(Long.parseLong(mapping.getAttribute("max-age")));
				}
				corsConfigurations.put(mapping.getAttribute("path"), config.applyPermitDefaultValues());
			}
		}

		MvcNamespaceUtils.registerCorsConfigurations(corsConfigurations, parserContext, parserContext.extractSource(element));
		return null;
	}

}
