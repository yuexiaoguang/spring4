package org.springframework.beans;

import java.beans.PropertyChangeEvent;

import org.springframework.util.ClassUtils;

/**
 * 尝试设置bean属性, 类型不匹配时抛出异常.
 */
@SuppressWarnings("serial")
public class TypeMismatchException extends PropertyAccessException {

	/**
	 * 注册类型不匹配错误的错误代码.
	 */
	public static final String ERROR_CODE = "typeMismatch";


	private transient Object value;

	private Class<?> requiredType;


	/**
	 * @param propertyChangeEvent 导致问题的PropertyChangeEvent
	 * @param requiredType 所需的目标类型
	 */
	public TypeMismatchException(PropertyChangeEvent propertyChangeEvent, Class<?> requiredType) {
		this(propertyChangeEvent, requiredType, null);
	}

	/**
	 * @param propertyChangeEvent 导致问题的PropertyChangeEvent
	 * @param requiredType 所需的目标类型 (or {@code null})
	 * @param cause the root cause (may be {@code null})
	 */
	public TypeMismatchException(PropertyChangeEvent propertyChangeEvent, Class<?> requiredType, Throwable cause) {
		super(propertyChangeEvent,
				"Failed to convert property value of type '" +
				ClassUtils.getDescriptiveType(propertyChangeEvent.getNewValue()) + "'" +
				(requiredType != null ?
				 " to required type '" + ClassUtils.getQualifiedName(requiredType) + "'" : "") +
				(propertyChangeEvent.getPropertyName() != null ?
				 " for property '" + propertyChangeEvent.getPropertyName() + "'" : ""),
				cause);
		this.value = propertyChangeEvent.getNewValue();
		this.requiredType = requiredType;
	}

	/**
	 * @param value 无法转换的违规值 (may be {@code null})
	 * @param requiredType 所需的目标类型 (or {@code null} if not known)
	 */
	public TypeMismatchException(Object value, Class<?> requiredType) {
		this(value, requiredType, null);
	}

	/**
	 * @param value 无法转换的违规值 (may be {@code null})
	 * @param requiredType 所需的目标类型 (or {@code null} if not known)
	 * @param cause the root cause (may be {@code null})
	 */
	public TypeMismatchException(Object value, Class<?> requiredType, Throwable cause) {
		super("Failed to convert value of type '" + ClassUtils.getDescriptiveType(value) + "'" +
				(requiredType != null ? " to required type '" + ClassUtils.getQualifiedName(requiredType) + "'" : ""),
				cause);
		this.value = value;
		this.requiredType = requiredType;
	}


	/**
	 * 返回违规的值 (may be {@code null}).
	 */
	@Override
	public Object getValue() {
		return this.value;
	}

	/**
	 * 所需的目标类型.
	 */
	public Class<?> getRequiredType() {
		return this.requiredType;
	}

	@Override
	public String getErrorCode() {
		return ERROR_CODE;
	}

}
