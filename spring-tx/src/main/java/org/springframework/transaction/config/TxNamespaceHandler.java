package org.springframework.transaction.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * {@code NamespaceHandler}允许使用XML或使用注解配置声明式事务管理.
 *
 * <p>此命名空间处理器是Spring事务管理工具中的核心功能, 并提供了两种以声明方式管理事务的方法.
 *
 * <p>一种方法是使用XML中使用{@code <tx:advice>}元素定义的事务语义,
 * 另一种方法是使用注解结合{@code <tx:annotation-driven>}元素.
 * 在Spring参考手册中, 两者都很详细.
 */
public class TxNamespaceHandler extends NamespaceHandlerSupport {

	static final String TRANSACTION_MANAGER_ATTRIBUTE = "transaction-manager";

	static final String DEFAULT_TRANSACTION_MANAGER_BEAN_NAME = "transactionManager";


	static String getTransactionManagerName(Element element) {
		return (element.hasAttribute(TRANSACTION_MANAGER_ATTRIBUTE) ?
				element.getAttribute(TRANSACTION_MANAGER_ATTRIBUTE) : DEFAULT_TRANSACTION_MANAGER_BEAN_NAME);
	}


	@Override
	public void init() {
		registerBeanDefinitionParser("advice", new TxAdviceBeanDefinitionParser());
		registerBeanDefinitionParser("annotation-driven", new AnnotationDrivenBeanDefinitionParser());
		registerBeanDefinitionParser("jta-transaction-manager", new JtaTransactionManagerBeanDefinitionParser());
	}

}
