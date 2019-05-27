package org.springframework.core.convert.converter;

/**
 * 用于"范围"转换器的工厂, 可以将对象从S转换为R的子类型.
 *
 * <p>实现可以另外实现{@link ConditionalConverter}.
 *
 * @param <S> 此工厂创建的源类型转换器
 * @param <R> 此工厂创建的目标范围 (或基础) 类型转换器;
 * 例如{@link Number}表示一组数字子类型.
 */
public interface ConverterFactory<S, R> {

	/**
	 * 获取从S转换为目标类型T的转换器, 其中T也是R的实例.
	 * 
	 * @param <T> 目标类型
	 * @param targetType 要转换为的目标类型
	 * 
	 * @return 从S到T的转换器
	 */
	<T extends R> Converter<S, T> getConverter(Class<T> targetType);

}
