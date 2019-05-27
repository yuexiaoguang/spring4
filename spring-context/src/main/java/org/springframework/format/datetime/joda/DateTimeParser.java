package org.springframework.format.datetime.joda;

import java.text.ParseException;
import java.util.Locale;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;

import org.springframework.format.Parser;

/**
 * 使用{@link DateTimeFormatter}解析Joda {@link DateTime}实例.
 */
public final class DateTimeParser implements Parser<DateTime> {

	private final DateTimeFormatter formatter;


	/**
	 * @param formatter Joda DateTimeFormatter实例
	 */
	public DateTimeParser(DateTimeFormatter formatter) {
		this.formatter = formatter;
	}


	@Override
	public DateTime parse(String text, Locale locale) throws ParseException {
		return JodaTimeContextHolder.getFormatter(this.formatter, locale).parseDateTime(text);
	}
}
