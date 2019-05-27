package org.springframework.format.datetime.joda;

import java.util.Locale;

import org.joda.time.ReadablePartial;
import org.joda.time.format.DateTimeFormatter;

import org.springframework.format.Printer;

/**
 * 使用{@link DateTimeFormatter}打印Joda-Time {@link ReadablePartial}实例.
 */
public final class ReadablePartialPrinter implements Printer<ReadablePartial> {

	private final DateTimeFormatter formatter;


	/**
	 * @param formatter Joda DateTimeFormatter实例
	 */
	public ReadablePartialPrinter(DateTimeFormatter formatter) {
		this.formatter = formatter;
	}


	@Override
	public String print(ReadablePartial partial, Locale locale) {
		return JodaTimeContextHolder.getFormatter(this.formatter, locale).print(partial);
	}

}
