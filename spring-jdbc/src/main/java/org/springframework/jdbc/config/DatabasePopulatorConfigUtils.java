package org.springframework.jdbc.config;

import java.util.List;

import org.w3c.dom.Element;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.jdbc.datasource.init.CompositeDatabasePopulator;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

class DatabasePopulatorConfigUtils {

	public static void setDatabasePopulator(Element element, BeanDefinitionBuilder builder) {
		List<Element> scripts = DomUtils.getChildElementsByTagName(element, "script");
		if (scripts.size() > 0) {
			builder.addPropertyValue("databasePopulator", createDatabasePopulator(element, scripts, "INIT"));
			builder.addPropertyValue("databaseCleaner", createDatabasePopulator(element, scripts, "DESTROY"));
		}
	}

	static private BeanDefinition createDatabasePopulator(Element element, List<Element> scripts, String execution) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(CompositeDatabasePopulator.class);

		boolean ignoreFailedDrops = element.getAttribute("ignore-failures").equals("DROPS");
		boolean continueOnError = element.getAttribute("ignore-failures").equals("ALL");

		ManagedList<BeanMetadataElement> delegates = new ManagedList<BeanMetadataElement>();
		for (Element scriptElement : scripts) {
			String executionAttr = scriptElement.getAttribute("execution");
			if (!StringUtils.hasText(executionAttr)) {
				executionAttr = "INIT";
			}
			if (!execution.equals(executionAttr)) {
				continue;
			}
			BeanDefinitionBuilder delegate = BeanDefinitionBuilder.genericBeanDefinition(ResourceDatabasePopulator.class);
			delegate.addPropertyValue("ignoreFailedDrops", ignoreFailedDrops);
			delegate.addPropertyValue("continueOnError", continueOnError);

			// 使用工厂bean作为资源, 以便在使用模式时为其提供排序
			BeanDefinitionBuilder resourcesFactory = BeanDefinitionBuilder.genericBeanDefinition(SortedResourcesFactoryBean.class);
			resourcesFactory.addConstructorArgValue(new TypedStringValue(scriptElement.getAttribute("location")));
			delegate.addPropertyValue("scripts", resourcesFactory.getBeanDefinition());
			if (StringUtils.hasLength(scriptElement.getAttribute("encoding"))) {
				delegate.addPropertyValue("sqlScriptEncoding", new TypedStringValue(scriptElement.getAttribute("encoding")));
			}
			String separator = getSeparator(element, scriptElement);
			if (separator != null) {
				delegate.addPropertyValue("separator", new TypedStringValue(separator));
			}
			delegates.add(delegate.getBeanDefinition());
		}
		builder.addPropertyValue("populators", delegates);

		return builder.getBeanDefinition();
	}

	private static String getSeparator(Element element, Element scriptElement) {
		String scriptSeparator = scriptElement.getAttribute("separator");
		if (StringUtils.hasLength(scriptSeparator)) {
			return scriptSeparator;
		}
		String elementSeparator = element.getAttribute("separator");
		if (StringUtils.hasLength(elementSeparator)) {
			return elementSeparator;
		}
		return null;
	}

}
