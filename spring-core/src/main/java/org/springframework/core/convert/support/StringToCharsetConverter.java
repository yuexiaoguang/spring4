package org.springframework.core.convert.support;

import java.nio.charset.Charset;

import org.springframework.core.convert.converter.Converter;

/**
 * 将字符串转换为{@link Charset}.
 */
class StringToCharsetConverter implements Converter<String, Charset> {

	@Override
	public Charset convert(String source) {
		return Charset.forName(source);
	}

}
