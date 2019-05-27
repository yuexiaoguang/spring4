package org.springframework.core.convert.converter;

/**
 * 用于使用类型转换系统注册转换器.
 */
public interface ConverterRegistry {

	/**
	 * 将普通转换器添加到此注册表.
	 * 派生自Converter的参数化类型的可转换的源/目标类型对.
	 * 
	 * @throws IllegalArgumentException 如果无法解析参数化类型
	 */
	void addConverter(Converter<?, ?> converter);

	/**
	 * 将普通转换器添加到此注册表.
	 * 可明确指定可转换的源/目标类型对.
	 * <p>允许转换器重用于多个不同的对, 而无需为每对创建一个Converter类.
	 */
	<S, T> void addConverter(Class<S> sourceType, Class<T> targetType, Converter<? super S, ? extends T> converter);

	/**
	 * 将泛型转换器添加到此注册表.
	 */
	void addConverter(GenericConverter converter);

	/**
	 * 将范围转换器工厂添加到此注册表.
	 * 派生自ConverterFactory的参数化类型的可转换的源/目标类型对.
	 * 
	 * @throws IllegalArgumentException 如果无法解析参数化类型
	 */
	void addConverterFactory(ConverterFactory<?, ?> factory);

	/**
	 * 删除所有从{@code sourceType}到{@code targetType}的转换器.
	 * 
	 * @param sourceType 源类型
	 * @param targetType 目标类型
	 */
	void removeConvertible(Class<?> sourceType, Class<?> targetType);

}
