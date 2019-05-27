package org.springframework.core.convert.converter;

import org.springframework.core.convert.TypeDescriptor;

/**
 * 允许{@link Converter}, {@link GenericConverter}或{@link ConverterFactory},
 * 根据{@code source}和{@code target} {@link TypeDescriptor}的属性有条件地执行.
 *
 * <p>通常用于基于字段或类级特征(例如注释或方法)来选择性地匹配自定义转换逻辑.
 * 例如, 当从String字段转换为Date字段时, 如果目标字段也已使用{@code @DateTimeFormat}注解, 则实现可能会返回{@code true}.
 *
 * <p>作为另一个示例, 当从String字段转换为{@code Account}字段时, 如果目标Account类定义了{@code public static findAccount(String)}方法,
 * 则实现可能返回{@code true}.
 */
public interface ConditionalConverter {

	/**
	 * 是否应选择从{@code sourceType}到{@code targetType}的转换?
	 * 
	 * @param sourceType 要转换的源字段的类型描述符
	 * @param targetType 要转换的目标字段的类型描述符
	 * 
	 * @return true 如果应该进行转换, 否则false
	 */
	boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType);

}
