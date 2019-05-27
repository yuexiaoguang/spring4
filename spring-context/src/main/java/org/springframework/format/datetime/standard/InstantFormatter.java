package org.springframework.format.datetime.standard;

import java.text.ParseException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.format.Formatter;
import org.springframework.lang.UsesJava8;

/**
 * JSR-310 {@link java.time.Instant}的{@link Formatter}实现, 遵循JSR-310的Instant解析规则
 * (也就是说, 不使用可配置的 {@link java.time.format.DateTimeFormatter}):
 * 接受默认的{@code ISO_INSTANT}格式以及{@code RFC_1123_DATE_TIME}
 * (通常用于HTTP日期标头值), 截至 Spring 4.3.
 */
@UsesJava8
public class InstantFormatter implements Formatter<Instant> {

	@Override
	public Instant parse(String text, Locale locale) throws ParseException {
		if (text.length() > 0 && Character.isDigit(text.charAt(0))) {
			// assuming UTC instant a la "2007-12-03T10:15:30.00Z"
			return Instant.parse(text);
		}
		else {
			// assuming RFC-1123 value a la "Tue, 3 Jun 2008 11:05:30 GMT"
			return Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(text));
		}
	}

	@Override
	public String print(Instant object, Locale locale) {
		return object.toString();
	}

}
