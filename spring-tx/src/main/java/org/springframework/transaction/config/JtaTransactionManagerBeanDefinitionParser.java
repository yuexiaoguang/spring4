package org.springframework.transaction.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;

/**
 * &lt;tx:jta-transaction-manager/&gt; XML配置元素的解析器,
 * 自动检测WebLogic和WebSphere服务器, 并公开相应的{@link org.springframework.transaction.jta.JtaTransactionManager}子类.
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
