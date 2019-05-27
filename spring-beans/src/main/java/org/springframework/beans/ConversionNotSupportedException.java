package org.springframework.beans;

import java.beans.PropertyChangeEvent;

/**
 * 如果找不到bean属性的合适编辑器或转换器，则抛出异常.
 */
@SuppressWarnings("serial")
public class ConversionNotSupportedException extends TypeMismatchException {

	/**
	 * @param propertyChangeEvent 导致问题的PropertyChangeEvent
	 * @param requiredType 所需的目标类型 (or {@code null} if not known)
	 * @param cause 根异常 (may be {@code null})
	 */
	public ConversionNotSupportedException(PropertyChangeEvent propertyChangeEvent,
			Class<?> requiredType, Throwable cause) {
		super(propertyChangeEvent, requiredType, cause);
	}

	/**
	 * @param value 无法转换的违规值 (may be {@code null})
	 * @param requiredType 所需的目标类型 (or {@code null} if not known)
	 * @param cause 根异常 (may be {@code null})
	 */
	public ConversionNotSupportedException(Object value, Class<?> requiredType, Throwable cause) {
		super(value, requiredType, cause);
	}

}
