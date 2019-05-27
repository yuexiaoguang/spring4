package org.springframework.core.convert.support;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalConverter;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.util.NumberUtils;

/**
 * 从任何JDK标准Number实现转换为任何其他JDK标准Number实现.
 *
 * <p>支持的Number类包括 Byte, Short, Integer, Float, Double, Long, BigInteger, BigDecimal.
 * 此类委托给{@link NumberUtils#convertNumberToTargetClass(Number, Class)}执行转换.
 */
final class NumberToNumberConverterFactory implements ConverterFactory<Number, Number>, ConditionalConverter {

	@Override
	public <T extends Number> Converter<Number, T> getConverter(Class<T> targetType) {
		return new NumberToNumber<T>(targetType);
	}

	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return !sourceType.equals(targetType);
	}


	private final static class NumberToNumber<T extends Number> implements Converter<Number, T> {

		private final Class<T> targetType;

		public NumberToNumber(Class<T> targetType) {
			this.targetType = targetType;
		}

		@Override
		public T convert(Number source) {
			return NumberUtils.convertNumberToTargetClass(source, this.targetType);
		}
	}

}
