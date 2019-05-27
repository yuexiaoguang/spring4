package org.springframework.beans;

import java.beans.PropertyChangeEvent;

/**
 * 与属性访问相关的异常的超类, 例如类型不匹配或调用目标异常.
 */
@SuppressWarnings({"serial", "deprecation"})
public abstract class PropertyAccessException extends BeansException implements org.springframework.core.ErrorCoded {

	private transient PropertyChangeEvent propertyChangeEvent;


	/**
	 * @param propertyChangeEvent 导致问题的PropertyChangeEvent
	 * @param msg the detail message
	 * @param cause the root cause
	 */
	public PropertyAccessException(PropertyChangeEvent propertyChangeEvent, String msg, Throwable cause) {
		super(msg, cause);
		this.propertyChangeEvent = propertyChangeEvent;
	}

	public PropertyAccessException(String msg, Throwable cause) {
		super(msg, cause);
	}


	/**
	 * 返回导致问题的PropertyChangeEvent.
	 * <p>可能是 {@code null}; 仅在实际的bean属性受到影响时才可用.
	 */
	public PropertyChangeEvent getPropertyChangeEvent() {
		return this.propertyChangeEvent;
	}

	/**
	 * 返回受影响的属性的名称.
	 */
	public String getPropertyName() {
		return (this.propertyChangeEvent != null ? this.propertyChangeEvent.getPropertyName() : null);
	}

	/**
	 * 返回即将设置的受影响值.
	 */
	public Object getValue() {
		return (this.propertyChangeEvent != null ? this.propertyChangeEvent.getNewValue() : null);
	}

	/**
	 * 返回此类异常的相应错误代码.
	 */
	@Override
	public abstract String getErrorCode();

}
