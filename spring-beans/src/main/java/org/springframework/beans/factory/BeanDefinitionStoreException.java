package org.springframework.beans.factory;

import org.springframework.beans.FatalBeanException;

/**
 * BeanFactory遇到无效的bean定义时抛出异常:
 * e.g. 如果bean元数据不完整或相互矛盾.
 */
@SuppressWarnings("serial")
public class BeanDefinitionStoreException extends FatalBeanException {

	private String resourceDescription;

	private String beanName;


	/**
	 * @param msg the detail message (used as exception message as-is)
	 */
	public BeanDefinitionStoreException(String msg) {
		super(msg);
	}

	/**
	 * @param msg the detail message (used as exception message as-is)
	 * @param cause the root cause (may be {@code null})
	 */
	public BeanDefinitionStoreException(String msg, Throwable cause) {
		super(msg, cause);
	}

	/**
	 * @param resourceDescription bean定义来自的资源的描述
	 * @param msg the detail message (used as exception message as-is)
	 */
	public BeanDefinitionStoreException(String resourceDescription, String msg) {
		super(msg);
		this.resourceDescription = resourceDescription;
	}

	/**
	 * @param resourceDescription bean定义来自的资源的描述
	 * @param msg the detail message (used as exception message as-is)
	 * @param cause the root cause (may be {@code null})
	 */
	public BeanDefinitionStoreException(String resourceDescription, String msg, Throwable cause) {
		super(msg, cause);
		this.resourceDescription = resourceDescription;
	}

	/**
	 * @param resourceDescription bean定义来自的资源的描述
	 * @param beanName the name of the bean
	 * @param msg the detail message (附加到介绍消息, 指示资源和bean的名称)
	 */
	public BeanDefinitionStoreException(String resourceDescription, String beanName, String msg) {
		this(resourceDescription, beanName, msg, null);
	}

	/**
	 * @param resourceDescription bean定义来自的资源的描述
	 * @param beanName the name of the bean
	 * @param msg the detail message (附加到介绍消息, 指示资源和bean的名称)
	 * @param cause the root cause (may be {@code null})
	 */
	public BeanDefinitionStoreException(String resourceDescription, String beanName, String msg, Throwable cause) {
		super("Invalid bean definition with name '" + beanName + "' defined in " + resourceDescription + ": " + msg,
				cause);
		this.resourceDescription = resourceDescription;
		this.beanName = beanName;
	}


	/**
	 * bean定义来自的资源的描述.
	 */
	public String getResourceDescription() {
		return this.resourceDescription;
	}

	/**
	 * bean的名称.
	 */
	public String getBeanName() {
		return this.beanName;
	}
}
