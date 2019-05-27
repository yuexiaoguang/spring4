package org.springframework.core.convert.support;

import org.springframework.core.convert.converter.Converter;

/**
 * 从任何JDK标准Number实现转换为Character.
 */
final class NumberToCharacterConverter implements Converter<Number, Character> {

	@Override
	public Character convert(Number source) {
		return (char) source.shortValue();
	}

}
