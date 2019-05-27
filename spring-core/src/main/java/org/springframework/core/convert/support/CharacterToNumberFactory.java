package org.springframework.core.convert.support;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.util.NumberUtils;

/**
 * 从Character转换为任何JDK标准Number实现.
 *
 * <p>支持的Number类包括 Byte, Short, Integer, Float, Double, Long, BigInteger, BigDecimal.
 * 此类委托给{@link NumberUtils#convertNumberToTargetClass(Number, Class)}以执行转换.
 */
final class CharacterToNumberFactory implements ConverterFactory<Character, Number> {

	@Override
	public <T extends Number> Converter<Character, T> getConverter(Class<T> targetType) {
		return new CharacterToNumber<T>(targetType);
	}

	private static final class CharacterToNumber<T extends Number> implements Converter<Character, T> {

		private final Class<T> targetType;

		public CharacterToNumber(Class<T> targetType) {
			this.targetType = targetType;
		}

		@Override
		public T convert(Character source) {
			return NumberUtils.convertNumberToTargetClass((short) source.charValue(), this.targetType);
		}
	}

}
