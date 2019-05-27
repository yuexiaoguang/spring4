package org.springframework.beans;

/**
 * 尝试设置不可写的属性的值时抛出异常 (通常是因为没有setter方法).
 */
@SuppressWarnings("serial")
public class NotWritablePropertyException extends InvalidPropertyException {

	private String[] possibleMatches = null;


	/**
	 * @param beanClass 违规的bean类
	 * @param propertyName 违规的属性名称
	 */
	public NotWritablePropertyException(Class<?> beanClass, String propertyName) {
		super(beanClass, propertyName,
				"Bean property '" + propertyName + "' is not writable or has an invalid setter method: " +
				"Does the return type of the getter match the parameter type of the setter?");
	}

	/**
	 * @param beanClass 违规的bean类
	 * @param propertyName 违规的属性名称
	 * @param msg the detail message
	 */
	public NotWritablePropertyException(Class<?> beanClass, String propertyName, String msg) {
		super(beanClass, propertyName, msg);
	}

	/**
	 * @param beanClass 违规的bean类
	 * @param propertyName 违规的属性名称
	 * @param msg the detail message
	 * @param cause the root cause
	 */
	public NotWritablePropertyException(Class<?> beanClass, String propertyName, String msg, Throwable cause) {
		super(beanClass, propertyName, msg, cause);
	}

	/**
	 * @param beanClass 违规的bean类
	 * @param propertyName 违规的属性名称
	 * @param msg the detail message
	 * @param possibleMatches 对与无效的属性名称非常匹配的实际bean属性名称的建议
	 */
	public NotWritablePropertyException(Class<?> beanClass, String propertyName, String msg, String[] possibleMatches) {
		super(beanClass, propertyName, msg);
		this.possibleMatches = possibleMatches;
	}


	/**
	 * 返回与无效的属性名称非常匹配的实际bean属性名称的建议.
	 */
	public String[] getPossibleMatches() {
		return this.possibleMatches;
	}

}
