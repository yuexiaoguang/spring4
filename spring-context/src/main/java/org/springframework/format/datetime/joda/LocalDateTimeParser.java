package org.springframework.format.datetime.joda;

import java.text.ParseException;
import java.util.Locale;

import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormatter;

import org.springframework.format.Parser;

/**
 * 使用{@link org.joda.time.format.DateTimeFormatter}解析Joda {@link org.joda.time.LocalDateTime}实例.
 */
public final class LocalDateTimeParser implements Parser<LocalDateTime> {

	private final DateTimeFormatter formatter;


	/**
	 * @param formatter Joda DateTimeFormatter实例
	 */
	public LocalDateTimeParser(DateTimeFormatter formatter) {
		this.formatter = formatter;
	}


	@Override
	public LocalDateTime parse(String text, Locale locale) throws ParseException {
		return JodaTimeContextHolder.getFormatter(this.formatter, locale).parseLocalDateTime(text);
	}

}
