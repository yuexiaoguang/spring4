package org.springframework.core.convert.support;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;

/**
 * 调用{@link Enum#ordinal()}将源Enum转换为Integer.
 * 此转换器不会将枚举与可转换的接口匹配.
 */
final class EnumToIntegerConverter extends AbstractConditionalEnumConverter implements Converter<Enum<?>, Integer> {

	public EnumToIntegerConverter(ConversionService conversionService) {
		super(conversionService);
	}

	@Override
	public Integer convert(Enum<?> source) {
		return source.ordinal();
	}

}
