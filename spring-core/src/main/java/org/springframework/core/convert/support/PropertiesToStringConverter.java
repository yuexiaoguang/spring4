package org.springframework.core.convert.support;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.springframework.core.convert.converter.Converter;

/**
 * 通过调用{@link Properties#store(java.io.OutputStream, String)}从Properties转换为String.
 * 在返回String之前使用ISO-8859-1 charset进行解码.
 */
final class PropertiesToStringConverter implements Converter<Properties, String> {

	@Override
	public String convert(Properties source) {
		try {
			ByteArrayOutputStream os = new ByteArrayOutputStream(256);
			source.store(os, null);
			return os.toString("ISO-8859-1");
		}
		catch (IOException ex) {
			// Should never happen.
			throw new IllegalArgumentException("Failed to store [" + source + "] into String", ex);
		}
	}
}
