package org.springframework.core.convert.support;

import java.io.ByteArrayInputStream;
import java.util.Properties;

import org.springframework.core.convert.converter.Converter;

/**
 * 通过调用 Properties#load(java.io.InputStream)将String转换为Properties.
 * 使用Properties所需的ISO-8559-1编码.
 */
final class StringToPropertiesConverter implements Converter<String, Properties> {

	@Override
	public Properties convert(String source) {
		try {
			Properties props = new Properties();
			// 必须使用ISO-8859-1编码, 因为 Properties.load(stream)需要它.
			props.load(new ByteArrayInputStream(source.getBytes("ISO-8859-1")));
			return props;
		}
		catch (Exception ex) {
			// Should never happen.
			throw new IllegalArgumentException("Failed to parse [" + source + "] into Properties", ex);
		}
	}

}
