package org.springframework.beans;

/**
 * 当有效的嵌套属性路径的导航遇到NullPointerException时抛出异常.
 *
 * <p>例如, 导航"spouse.age"可能会失败, 因为目标对象的配偶属性具有null值.
 */
@SuppressWarnings("serial")
public class NullValueInNestedPathException extends InvalidPropertyException {

	/**
	 * @param beanClass 违规的bean类
	 * @param propertyName 违规的属性
	 */
	public NullValueInNestedPathException(Class<?> beanClass, String propertyName) {
		super(beanClass, propertyName, "Value of nested property '" + propertyName + "' is null");
	}

	/**
	 * @param beanClass 违规的bean类
	 * @param propertyName 违规的属性
	 * @param msg the detail message
	 */
	public NullValueInNestedPathException(Class<?> beanClass, String propertyName, String msg) {
		super(beanClass, propertyName, msg);
	}

	/**
	 * @param beanClass 违规的bean类
	 * @param propertyName 违规的属性
	 * @param msg the detail message
	 * @param cause the root cause
	 */
	public NullValueInNestedPathException(Class<?> beanClass, String propertyName, String msg, Throwable cause) {
		super(beanClass, propertyName, msg, cause);
	}

}
