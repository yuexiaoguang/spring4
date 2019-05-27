package org.springframework.jdbc.config;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseFactoryBean;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.util.StringUtils;

import org.w3c.dom.Element;

/**
 * {@link org.springframework.beans.factory.xml.BeanDefinitionParser},
 * 解析{@code embedded-database}元素并为{@link EmbeddedDatabaseFactoryBean}创建{@link BeanDefinition}.
 *
 * <p>挑选嵌套的{@code script}元素, 并为每个元素配置{@link ResourceDatabasePopulator}.
 */
class EmbeddedDatabaseBeanDefinitionParser extends AbstractBeanDefinitionParser {

	/**
	 * "database-name"属性的常量.
	 */
	static final String DB_NAME_ATTRIBUTE = "database-name";

	/**
	 * "generate-name"属性的常量.
	 */
	static final String GENERATE_NAME_ATTRIBUTE = "generate-name";


	@Override
	protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(EmbeddedDatabaseFactoryBean.class);
		setGenerateUniqueDatabaseNameFlag(element, builder);
		setDatabaseName(element, builder);
		setDatabaseType(element, builder);
		DatabasePopulatorConfigUtils.setDatabasePopulator(element, builder);
		builder.getRawBeanDefinition().setSource(parserContext.extractSource(element));
		return builder.getBeanDefinition();
	}

	@Override
	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	private void setGenerateUniqueDatabaseNameFlag(Element element, BeanDefinitionBuilder builder) {
		String generateName = element.getAttribute(GENERATE_NAME_ATTRIBUTE);
		if (StringUtils.hasText(generateName)) {
			builder.addPropertyValue("generateUniqueDatabaseName", generateName);
		}
	}

	private void setDatabaseName(Element element, BeanDefinitionBuilder builder) {
		// 1) 检查显式数据库名称
		String name = element.getAttribute(DB_NAME_ATTRIBUTE);

		// 2) 回退到基于ID的隐式数据库名称
		if (!StringUtils.hasText(name)) {
			name = element.getAttribute(ID_ATTRIBUTE);
		}

		if (StringUtils.hasText(name)) {
			builder.addPropertyValue("databaseName", name);
		}
		// 否则, 让EmbeddedDatabaseFactory使用默认的"testdb"名称
	}

	private void setDatabaseType(Element element, BeanDefinitionBuilder builder) {
		String type = element.getAttribute("type");
		if (StringUtils.hasText(type)) {
			builder.addPropertyValue("databaseType", type);
		}
	}
}
