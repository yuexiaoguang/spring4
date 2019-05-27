package org.springframework.expression;

import org.springframework.core.convert.TypeDescriptor;

/**
 * 类型转换器, 可以转换表达式评估期间遇到的不同类型之间的值.
 * 这是表达式解析器的SPI;
 * 请参阅{@link org.springframework.core.convert.ConversionService}以获取Spring转换工具的主要用户API.
 */
public interface TypeConverter {

	/**
	 * 如果类型转换器可以将指定类型转换为所需的目标类型, 则返回{@code true}.
	 * 
	 * @param sourceType 描述源类型的类型描述符
	 * @param targetType 描述请求的结果类型的类型描述符
	 * 
	 * @return {@code true} 如果可以执行转换
	 */
	boolean canConvert(TypeDescriptor sourceType, TypeDescriptor targetType);

	/**
	 * 将值从一种类型转换(或强制)到另一种类型, 例如从{@code boolean}转换为{@code String}.
	 * <p>{@link TypeDescriptor}参数支持对类型集合的支持:
	 * 例如, 调用者需要{@code List&lt;Integer&gt;}, 而不是 {@code List}.
	 * 
	 * @param value 要转换的值
	 * @param sourceType 提供有关源对象的额外信息的类型描述符
	 * @param targetType 一个类型描述符, 提供有关请求的结果类型的额外信息
	 * 
	 * @return 转换后的值
	 * @throws EvaluationException 如果转换失败或无法开始
	 */
	Object convertValue(Object value, TypeDescriptor sourceType, TypeDescriptor targetType);

}
