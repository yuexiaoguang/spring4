package org.springframework.beans.factory.config;

import org.springframework.beans.factory.NamedBean;
import org.springframework.util.Assert;

/**
 * 给定bean名称和bean实例的简单持有者.
 */
public class NamedBeanHolder<T> implements NamedBean {

	private final String beanName;

	private final T beanInstance;


	/**
	 * @param beanName bean的名称
	 * @param beanInstance 对应的bean实例
	 */
	public NamedBeanHolder(String beanName, T beanInstance) {
		Assert.notNull(beanName, "Bean name must not be null");
		this.beanName = beanName;
		this.beanInstance = beanInstance;
	}


	/**
	 * 返回bean的名称 (never {@code null}).
	 */
	@Override
	public String getBeanName() {
		return this.beanName;
	}

	/**
	 * 返回相应的bean实例 (can be {@code null}).
	 */
	public T getBeanInstance() {
		return this.beanInstance;
	}

}
