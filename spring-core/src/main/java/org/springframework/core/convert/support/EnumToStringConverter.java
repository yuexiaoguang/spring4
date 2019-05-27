package org.springframework.core.convert.support;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;

/**
 * 调用{@link Enum#name()}将源Enum转换为String.
 * 此转换器不会将枚举与可转换的接口匹配.
 */
final class EnumToStringConverter extends AbstractConditionalEnumConverter implements Converter<Enum<?>, String> {

	public EnumToStringConverter(ConversionService conversionService) {
		super(conversionService);
	}

	@Override
	public String convert(Enum<?> source) {
		return source.name();
	}

}
