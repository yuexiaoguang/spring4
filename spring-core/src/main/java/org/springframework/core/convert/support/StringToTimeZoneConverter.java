package org.springframework.core.convert.support;

import java.util.TimeZone;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.UsesJava8;
import org.springframework.util.StringUtils;

/**
 * 将字符串转换为{@link TimeZone}.
 */
@UsesJava8
class StringToTimeZoneConverter implements Converter<String, TimeZone> {

	@Override
	public TimeZone convert(String source) {
		return StringUtils.parseTimeZoneString(source);
	}

}
