package org.springframework.core.convert.support;

import java.util.Locale;

import org.springframework.core.convert.converter.Converter;
import org.springframework.util.StringUtils;

/**
 * 从String转换为{@link java.util.Locale}.
 */
final class StringToLocaleConverter implements Converter<String, Locale> {

	@Override
	public Locale convert(String source) {
		return StringUtils.parseLocaleString(source);
	}

}
