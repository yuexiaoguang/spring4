package org.springframework.core.convert;

import org.springframework.util.ObjectUtils;

/**
 * 实际类型转换尝试失败时抛出的异常.
 */
@SuppressWarnings("serial")
public class ConversionFailedException extends ConversionException {

	private final TypeDescriptor sourceType;

	private final TypeDescriptor targetType;

	private final Object value;


	/**
	 * @param sourceType 值的原始类型
	 * @param targetType 值的目标类型
	 * @param value 试图转换的值
	 * @param cause 转换失败的原因
	 */
	public ConversionFailedException(TypeDescriptor sourceType, TypeDescriptor targetType, Object value, Throwable cause) {
		super("Failed to convert from type [" + sourceType + "] to type [" + targetType +
				"] for value '" + ObjectUtils.nullSafeToString(value) + "'", cause);
		this.sourceType = sourceType;
		this.targetType = targetType;
		this.value = value;
	}


	/**
	 * 返回尝试转换值的源类型.
	 */
	public TypeDescriptor getSourceType() {
		return this.sourceType;
	}

	/**
	 * 返回尝试将值转换为的目标类型.
	 */
	public TypeDescriptor getTargetType() {
		return this.targetType;
	}

	/**
	 * 返回有问题的值.
	 */
	public Object getValue() {
		return this.value;
	}

}
