package org.springframework.format.datetime.joda;

import java.util.Locale;

import org.joda.time.ReadableInstant;
import org.joda.time.format.DateTimeFormatter;

import org.springframework.format.Printer;

/**
 * 使用{@link DateTimeFormatter}打印Joda-Time {@link ReadableInstant}实例.
 */
public final class ReadableInstantPrinter implements Printer<ReadableInstant> {

	private final DateTimeFormatter formatter;


	/**
	 * @param formatter Joda DateTimeFormatter实例
	 */
	public ReadableInstantPrinter(DateTimeFormatter formatter) {
		this.formatter = formatter;
	}


	@Override
	public String print(ReadableInstant instant, Locale locale) {
		return JodaTimeContextHolder.getFormatter(this.formatter, locale).print(instant);
	}

}
