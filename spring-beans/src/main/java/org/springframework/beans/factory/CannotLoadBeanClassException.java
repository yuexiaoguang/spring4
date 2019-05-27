package org.springframework.beans.factory;

import org.springframework.beans.FatalBeanException;

/**
 * 当BeanFactory无法加载给定bean的指定类时抛出异常.
 */
@SuppressWarnings("serial")
public class CannotLoadBeanClassException extends FatalBeanException {

	private String resourceDescription;

	private String beanName;

	private String beanClassName;


	/**
	 * @param resourceDescription bean定义来自的资源的描述
	 * @param beanName 请求的bean的名称
	 * @param beanClassName bean类的名称
	 * @param cause the root cause
	 */
	public CannotLoadBeanClassException(
			String resourceDescription, String beanName, String beanClassName, ClassNotFoundException cause) {

		super("Cannot find class [" + beanClassName + "] for bean with name '" + beanName + "'" +
				(resourceDescription != null ? " defined in " + resourceDescription : ""), cause);
		this.resourceDescription = resourceDescription;
		this.beanName = beanName;
		this.beanClassName = beanClassName;
	}

	/**
	 * @param resourceDescription bean定义来自的资源的描述
	 * @param beanName 请求的bean的名称
	 * @param beanClassName bean类的名称
	 * @param cause the root cause
	 */
	public CannotLoadBeanClassException(
			String resourceDescription, String beanName, String beanClassName, LinkageError cause) {

		super("Error loading class [" + beanClassName + "] for bean with name '" + beanName + "'" +
				(resourceDescription != null ? " defined in " + resourceDescription : "") +
				": problem with class file or dependent class", cause);
		this.resourceDescription = resourceDescription;
		this.beanName = beanName;
		this.beanClassName = beanClassName;
	}


	/**
	 * 返回bean定义来自的资源的描述.
	 */
	public String getResourceDescription() {
		return this.resourceDescription;
	}

	/**
	 * 返回请求的bean的名称.
	 */
	public String getBeanName() {
		return this.beanName;
	}

	/**
	 * 返回尝试加载的类的名称.
	 */
	public String getBeanClassName() {
		return this.beanClassName;
	}
}
