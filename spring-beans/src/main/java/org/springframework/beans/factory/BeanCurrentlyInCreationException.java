package org.springframework.beans.factory;

/**
 * 在引用当前正在创建的bean的情况下抛出异常.
 * 通常在构造函数自动装配与当前正在构造的bean匹配时发生.
 */
@SuppressWarnings("serial")
public class BeanCurrentlyInCreationException extends BeanCreationException {

	/**
	 * @param beanName 请求的bean的名称
	 */
	public BeanCurrentlyInCreationException(String beanName) {
		super(beanName,
				"Requested bean is currently in creation: Is there an unresolvable circular reference?");
	}

	/**
	 * @param beanName 请求的bean的名称
	 * @param msg the detail message
	 */
	public BeanCurrentlyInCreationException(String beanName, String msg) {
		super(beanName, msg);
	}

}
