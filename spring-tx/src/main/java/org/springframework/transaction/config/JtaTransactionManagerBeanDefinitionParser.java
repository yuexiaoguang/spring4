package org.springframework.transaction.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;

/**
 * Parser for the &lt;tx:jta-transaction-manager/&gt; XML configuration element,
 * autodetecting WebLogic and WebSphere servers and exposing the corresponding
 * {@link org.springframework.transaction.jta.JtaTransactionManager} subclass.
 */
public class JtaTransactionManagerBeanDefinitionParser extends AbstractSingleBeanDefinitionParser  {

	@Override
	protected String getBeanClassName(Element element) {
		return JtaTransactionManagerFactoryBean.resolveJtaTransactionManagerClassName();
	}

	@Override
	protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) {
		return TxNamespaceHandler.DEFAULT_TRANSACTION_MANAGER_BEAN_NAME;
	}

}
