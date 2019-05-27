package org.springframework.core.convert;

/**
 * 在给定的转换服务中找不到合适的转换器时抛出的异常.
 */
@SuppressWarnings("serial")
public class ConverterNotFoundException extends ConversionException {

	private final TypeDescriptor sourceType;

	private final TypeDescriptor targetType;


	/**
	 * @param sourceType 请求转换的源类型
	 * @param targetType 请求转换为的目标类型
	 */
	public ConverterNotFoundException(TypeDescriptor sourceType, TypeDescriptor targetType) {
		super("No converter found capable of converting from type [" + sourceType + "] to type [" + targetType + "]");
		this.sourceType = sourceType;
		this.targetType = targetType;
	}


	/**
	 * 返回请求转换的源类型.
	 */
	public TypeDescriptor getSourceType() {
		return this.sourceType;
	}

	/**
	 * 返回请求转换为的目标类型.
	 */
	public TypeDescriptor getTargetType() {
		return this.targetType;
	}

}
