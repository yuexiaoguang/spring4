package org.springframework.core.convert.support;

import java.util.UUID;

import org.springframework.core.convert.converter.Converter;
import org.springframework.util.StringUtils;

/**
 * 从String转换为{@link java.util.UUID}.
 */
final class StringToUUIDConverter implements Converter<String, UUID> {

	@Override
	public UUID convert(String source) {
		return (StringUtils.hasLength(source) ? UUID.fromString(source.trim()) : null);
	}

}
