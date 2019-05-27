package org.springframework.core.convert.support;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.util.NumberUtils;

/**
 * 从String转换为JDK标准的Number实现.
 *
 * <p>支持的Number类包括 Byte, Short, Integer, Float, Double, Long, BigInteger, BigDecimal.
 * 此类委托给{@link NumberUtils#parseNumber(String, Class)}执行转换.
 */
final class StringToNumberConverterFactory implements ConverterFactory<String, Number> {

	@Override
	public <T extends Number> Converter<String, T> getConverter(Class<T> targetType) {
		return new StringToNumber<T>(targetType);
	}


	private static final class StringToNumber<T extends Number> implements Converter<String, T> {

		private final Class<T> targetType;

		public StringToNumber(Class<T> targetType) {
			this.targetType = targetType;
		}

		@Override
		public T convert(String source) {
			if (source.isEmpty()) {
				return null;
			}
			return NumberUtils.parseNumber(source, this.targetType);
		}
	}

}
