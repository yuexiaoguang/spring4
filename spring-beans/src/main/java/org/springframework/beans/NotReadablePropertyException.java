package org.springframework.beans;

/**
 * 尝试获取不可读的属性值时抛出异常, 因为没有getter方法.
 */
@SuppressWarnings("serial")
public class NotReadablePropertyException extends InvalidPropertyException {

	/**
	 * @param beanClass 违规的bean类
	 * @param propertyName 违规的属性名称
	 */
	public NotReadablePropertyException(Class<?> beanClass, String propertyName) {
		super(beanClass, propertyName,
				"Bean property '" + propertyName + "' is not readable or has an invalid getter method: " +
				"Does the return type of the getter match the parameter type of the setter?");
	}

	/**
	 * @param beanClass 违规的bean类
	 * @param propertyName 违规的属性名称
	 * @param msg the detail message
	 */
	public NotReadablePropertyException(Class<?> beanClass, String propertyName, String msg) {
		super(beanClass, propertyName, msg);
	}

	/**
	 * @param beanClass 违规的bean类
	 * @param propertyName 违规的属性名称
	 * @param msg the detail message
	 * @param cause the root cause
	 */
	public NotReadablePropertyException(Class<?> beanClass, String propertyName, String msg, Throwable cause) {
		super(beanClass, propertyName, msg, cause);
	}

}
