package org.springframework.beans.factory;

import org.springframework.beans.BeansException;
import org.springframework.util.ClassUtils;

/**
 * 当bean与预期类型不匹配时抛出.
 */
@SuppressWarnings("serial")
public class BeanNotOfRequiredTypeException extends BeansException {

	/** 错误类型的实例的名称 */
	private String beanName;

	/** 所需类型 */
	private Class<?> requiredType;

	/** 错误的类型 */
	private Class<?> actualType;


	/**
	 * @param beanName 请求的bean的名称
	 * @param requiredType 所需类型
	 * @param actualType 返回的实际类型, 与预期的类型不符
	 */
	public BeanNotOfRequiredTypeException(String beanName, Class<?> requiredType, Class<?> actualType) {
		super("Bean named '" + beanName + "' is expected to be of type '" + ClassUtils.getQualifiedName(requiredType) +
				"' but was actually of type '" + ClassUtils.getQualifiedName(actualType) + "'");
		this.beanName = beanName;
		this.requiredType = requiredType;
		this.actualType = actualType;
	}


	/**
	 * 返回错误类型的实例的名称.
	 */
	public String getBeanName() {
		return this.beanName;
	}

	/**
	 * 返回bean的预期类型.
	 */
	public Class<?> getRequiredType() {
		return this.requiredType;
	}

	/**
	 * 返回找到的实例的实际类型.
	 */
	public Class<?> getActualType() {
		return this.actualType;
	}

}
