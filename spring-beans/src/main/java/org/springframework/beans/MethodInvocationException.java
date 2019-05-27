package org.springframework.beans;

import java.beans.PropertyChangeEvent;

/**
 * 当bean属性getter或setter方法抛出异常时抛出, 类似于InvocationTargetException.
 */
@SuppressWarnings("serial")
public class MethodInvocationException extends PropertyAccessException {

	/**
	 * 将注册方法调用错误的错误代码.
	 */
	public static final String ERROR_CODE = "methodInvocation";


	/**
	 * @param propertyChangeEvent 导致异常的PropertyChangeEvent
	 * @param cause the Throwable raised by the invoked method
	 */
	public MethodInvocationException(PropertyChangeEvent propertyChangeEvent, Throwable cause) {
		super(propertyChangeEvent, "Property '" + propertyChangeEvent.getPropertyName() + "' threw exception", cause);
	}

	@Override
	public String getErrorCode() {
		return ERROR_CODE;
	}

}
