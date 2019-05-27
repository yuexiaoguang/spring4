package org.springframework.beans.factory.access.el;

import javax.el.ELContext;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.util.Assert;

/**
 * {@link SpringBeanELResolver}的简单具体变体, 委托给给定的 {@link BeanFactory}.
 */
public class SimpleSpringBeanELResolver extends SpringBeanELResolver {

	private final BeanFactory beanFactory;


	/**
	 * @param beanFactory 委托的Spring BeanFactory
	 */
	public SimpleSpringBeanELResolver(BeanFactory beanFactory) {
		Assert.notNull(beanFactory, "BeanFactory must not be null");
		this.beanFactory = beanFactory;
	}

	@Override
	protected BeanFactory getBeanFactory(ELContext elContext) {
		return this.beanFactory;
	}

}
