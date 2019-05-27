package org.springframework.beans;

/**
 * 引用无效的bean属性时抛出异常. 包含违规的bean类和属性名称.
 */
@SuppressWarnings("serial")
public class InvalidPropertyException extends FatalBeanException {

	private Class<?> beanClass;

	private String propertyName;


	/**
	 * @param beanClass 违规的bean类
	 * @param propertyName 违规的属性名称
	 * @param msg the detail message
	 */
	public InvalidPropertyException(Class<?> beanClass, String propertyName, String msg) {
		this(beanClass, propertyName, msg, null);
	}

	/**
	 * @param beanClass 违规的bean类
	 * @param propertyName 违规的属性名称
	 * @param msg the detail message
	 * @param cause the root cause
	 */
	public InvalidPropertyException(Class<?> beanClass, String propertyName, String msg, Throwable cause) {
		super("Invalid property '" + propertyName + "' of bean class [" + beanClass.getName() + "]: " + msg, cause);
		this.beanClass = beanClass;
		this.propertyName = propertyName;
	}

	/**
	 * 返回有问题的bean类.
	 */
	public Class<?> getBeanClass() {
		return beanClass;
	}

	/**
	 * 返回违规的属性名称.
	 */
	public String getPropertyName() {
		return propertyName;
	}

}
