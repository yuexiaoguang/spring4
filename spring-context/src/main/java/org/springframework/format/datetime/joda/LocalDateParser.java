package org.springframework.format.datetime.joda;

import java.text.ParseException;
import java.util.Locale;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormatter;

import org.springframework.format.Parser;

/**
 * 使用{@link org.joda.time.format.DateTimeFormatter}解析Joda {@link org.joda.time.LocalDate}实例.
 */
public final class LocalDateParser implements Parser<LocalDate> {

	private final DateTimeFormatter formatter;


	/**
	 * @param formatter Joda DateTimeFormatter实例
	 */
	public LocalDateParser(DateTimeFormatter formatter) {
		this.formatter = formatter;
	}


	@Override
	public LocalDate parse(String text, Locale locale) throws ParseException {
		return JodaTimeContextHolder.getFormatter(this.formatter, locale).parseLocalDate(text);
	}

}
