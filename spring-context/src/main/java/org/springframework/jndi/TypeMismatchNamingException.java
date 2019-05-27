package org.springframework.jndi;

import javax.naming.NamingException;

/**
 * 如果遇到位于JNDI环境中的对象的类型不匹配, 则抛出异常. 由JndiTemplate抛出.
 */
@SuppressWarnings("serial")
public class TypeMismatchNamingException extends NamingException {

	private Class<?> requiredType;

	private Class<?> actualType;


	/**
	 * @param jndiName JNDI名称
	 * @param requiredType 查找所需的类型
	 * @param actualType 查找返回的实际类型
	 */
	public TypeMismatchNamingException(String jndiName, Class<?> requiredType, Class<?> actualType) {
		super("Object of type [" + actualType + "] available at JNDI location [" +
				jndiName + "] is not assignable to [" + requiredType.getName() + "]");
		this.requiredType = requiredType;
		this.actualType = actualType;
	}

	@Deprecated
	public TypeMismatchNamingException(String explanation) {
		super(explanation);
	}


	/**
	 * 如果可用, 返回查找所需的类型.
	 */
	public final Class<?> getRequiredType() {
		return this.requiredType;
	}

	/**
	 * 返回查找返回的实际类型.
	 */
	public final Class<?> getActualType() {
		return this.actualType;
	}

}
