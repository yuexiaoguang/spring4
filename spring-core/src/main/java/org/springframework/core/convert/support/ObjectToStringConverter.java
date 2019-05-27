package org.springframework.core.convert.support;

import org.springframework.core.convert.converter.Converter;

/**
 * 调用{@link Object#toString()}将源Object转换为String.
 */
final class ObjectToStringConverter implements Converter<Object, String> {

	@Override
	public String convert(Object source) {
		return source.toString();
	}

}
