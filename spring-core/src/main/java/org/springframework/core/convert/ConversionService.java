package org.springframework.core.convert;

/**
 * 用于类型转换的服务接口. 这是转换系统的切入点.
 * 调用{@link #convert(Object, Class)}以使用此系统执行线程安全类型转换.
 */
public interface ConversionService {

	/**
	 * 如果{@code sourceType}的对象可以转换为{@code targetType}, 则返回{@code true}.
	 * <p>如果此方法返回{@code true}, 则表示{@link #convert(Object, Class)}能够将{@code sourceType}的实例转换为{@code targetType}.
	 * <p>关于集合, 数组和Map类型的特别说明:
	 * 对于集合, 数组和Map类型之间的转换, 此方法将返回{@code true},
	 * 即使转换调用仍然可能生成{@link ConversionException}, 如果底层元素不可转换.
	 * 在处理集合和Map时, 呼叫者应该处理这种特殊情.
	 * 
	 * @param sourceType 要转换的源类型 (如果源为{@code null}, 则可能为{@code null})
	 * @param targetType 要转换为的目标类型 (必需)
	 * 
	 * @return {@code true} 如果可以执行转换, 否则{@code false}
	 * @throws IllegalArgumentException 如果{@code targetType}是{@code null}
	 */
	boolean canConvert(Class<?> sourceType, Class<?> targetType);

	/**
	 * 如果{@code sourceType}的对象可以转换为{@code targetType}, 则返回{@code true}.
	 * TypeDescriptors提供有关发生转换的源位置和目标位置的附加上下文, 通常是对象字段或属性位置.
	 * <p>如果此方法返回{@code true}, 则表示{@link #convert(Object, TypeDescriptor, TypeDescriptor)}
	 * 能够将{@code sourceType}的实例转换为{@code targetType}.
	 * <p>关于集合, 数组和Map类型的特别说明:
	 * 对于集合, 数组和Map类型之间的转换, 此方法将返回{@code true},
	 * 即使转换调用仍然可能生成{@link ConversionException}, 如果底层元素不可转换.
	 * 在处理集合和Map时, 呼叫者应该处理这种特殊情.
	 * 
	 * @param sourceType 要转换的源类型 (如果源为{@code null}, 则可能为{@code null})
	 * @param targetType 要转换为的目标类型 (必需)
	 * 
	 * @return {@code true} 如果可以执行转换, 否则{@code false}
	 * @throws IllegalArgumentException 如果{@code targetType}是{@code null}
	 */
	boolean canConvert(TypeDescriptor sourceType, TypeDescriptor targetType);

	/**
	 * 将给定的{@code source}转换为指定的{@code targetType}.
	 * 
	 * @param source 要转换的源对象 (may be {@code null})
	 * @param targetType 要转换为的目标类型 (必须)
	 * 
	 * @return 转换后的对象, targetType的一个实例
	 * @throws ConversionException 如果发生转换异常
	 * @throws IllegalArgumentException 如果targetType是 {@code null}
	 */
	<T> T convert(Object source, Class<T> targetType);

	/**
	 * 将给定的{@code source}转换为指定的{@code targetType}.
	 * TypeDescriptors提供有关将发生转换的源位置和目标位置的附加上下文, 通常是对象字段或属性位置.
	 * 
	 * @param source 要转换的源对象 (may be {@code null})
	 * @param sourceType 有关要转换的源类型的上下文 (如果source为{@code null}, 则可能为{@code null})
	 * @param targetType 有关要转换为的目标类型的上下文 (必须)
	 * 
	 * @return 转换后的对象, {@link TypeDescriptor#getObjectType() targetType}的一个实例
	 * @throws ConversionException 如果发生转换异常
	 * @throws IllegalArgumentException 如果targetType是 {@code null}, 或{@code sourceType}是{@code null}, 但source不是{@code null}
	 */
	Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType);

}
