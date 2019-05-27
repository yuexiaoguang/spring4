package org.springframework.jdbc.config;

import org.springframework.beans.factory.xml.NamespaceHandler;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * 用于JDBC配置命名空间的{@link NamespaceHandler}.
 */
public class JdbcNamespaceHandler extends NamespaceHandlerSupport {

	@Override
	public void init() {
		registerBeanDefinitionParser("embedded-database", new EmbeddedDatabaseBeanDefinitionParser());
		registerBeanDefinitionParser("initialize-database", new InitializeDatabaseBeanDefinitionParser());
	}
}
