package org.springframework.core.convert.converter;

import java.util.Comparator;
import java.util.Map;

import org.springframework.core.convert.ConversionService;
import org.springframework.util.Assert;
import org.springframework.util.comparator.ComparableComparator;

/**
 * 在比较之前转换值的{@link Comparator}.
 * 指定的{@link Converter}将用于在传递给底层{@code Comparator}之前转换每个值.
 *
 * @param <S> 源类型
 * @param <T> 目标类型
 */
public class ConvertingComparator<S, T> implements Comparator<S> {

	private final Comparator<T> comparator;

	private final Converter<S, T> converter;


	/**
	 * @param converter 转换器
	 */
	@SuppressWarnings("unchecked")
	public ConvertingComparator(Converter<S, T> converter) {
		this(ComparableComparator.INSTANCE, converter);
	}

	/**
	 * @param comparator 用于比较转换后的值的底层比较器
	 * @param converter 转换器
	 */
	public ConvertingComparator(Comparator<T> comparator, Converter<S, T> converter) {
		Assert.notNull(comparator, "Comparator must not be null");
		Assert.notNull(converter, "Converter must not be null");
		this.comparator = comparator;
		this.converter = converter;
	}

	/**
	 * @param comparator 底层比较器
	 * @param conversionService 转换服务
	 * @param targetType 目标类型
	 */
	public ConvertingComparator(
			Comparator<T> comparator, ConversionService conversionService, Class<? extends T> targetType) {

		this(comparator, new ConversionServiceConverter<S, T>(conversionService, targetType));
	}


	@Override
	public int compare(S o1, S o2) {
		T c1 = this.converter.convert(o1);
		T c2 = this.converter.convert(o2);
		return this.comparator.compare(c1, c2);
	}

	/**
	 * 创建一个新的{@link ConvertingComparator},
	 * 根据{@link java.util.Map.Entry#getKey() keys}比较{@link java.util.Map.Entry map * entries}.
	 * 
	 * @param comparator 用于比较Key的底层比较器
	 * 
	 * @return 新的{@link ConvertingComparator}实例
	 */
	public static <K, V> ConvertingComparator<Map.Entry<K, V>, K> mapEntryKeys(Comparator<K> comparator) {
		return new ConvertingComparator<Map.Entry<K,V>, K>(comparator, new Converter<Map.Entry<K, V>, K>() {
			@Override
			public K convert(Map.Entry<K, V> source) {
				return source.getKey();
			}
		});
	}

	/**
	 * 创建一个新的{@link ConvertingComparator},
	 * 根据{@link java.util.Map.Entry#getValue() values}比较{@link java.util.Map.Entry map entries}.
	 * 
	 * @param comparator 用于比较值的底层比较器
	 * 
	 * @return 新的{@link ConvertingComparator}实例
	 */
	public static <K, V> ConvertingComparator<Map.Entry<K, V>, V> mapEntryValues(Comparator<V> comparator) {
		return new ConvertingComparator<Map.Entry<K,V>, V>(comparator, new Converter<Map.Entry<K, V>, V>() {
			@Override
			public V convert(Map.Entry<K, V> source) {
				return source.getValue();
			}
		});
	}


	/**
	 * 将{@link ConversionService}和<tt>targetType</tt>适配为{@link Converter}.
	 */
	private static class ConversionServiceConverter<S, T> implements Converter<S, T> {

		private final ConversionService conversionService;

		private final Class<? extends T> targetType;

		public ConversionServiceConverter(ConversionService conversionService,
			Class<? extends T> targetType) {
			Assert.notNull(conversionService, "ConversionService must not be null");
			Assert.notNull(targetType, "TargetType must not be null");
			this.conversionService = conversionService;
			this.targetType = targetType;
		}

		@Override
		public T convert(S source) {
			return this.conversionService.convert(source, this.targetType);
		}
	}

}
