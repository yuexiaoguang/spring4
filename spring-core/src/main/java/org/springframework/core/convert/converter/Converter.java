package org.springframework.core.convert.converter;

/**
 * 将{@code S}类型的源对象转换为{@code T}类型的目标的转换器.
 *
 * <p>此接口的实现是线程安全的, 可以共享.
 *
 * <p>实现可以另外实现{@link ConditionalConverter}.
 *
 * @param <S> 源类型
 * @param <T> 目标类型
 */
public interface Converter<S, T> {

	/**
	 * 将{@code S}类型的源对象转换为目标类型{@code T}.
	 * 
	 * @param source 要转换的源对象, 它必须是{@code S}的实例 (never {@code null})
	 * 
	 * @return 转换后的对象, 必须是{@code T}的实例 (可能是 {@code null})
	 * @throws IllegalArgumentException 如果源无法转换为所需的目标类型
	 */
	T convert(S source);

}
