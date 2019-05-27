package org.springframework.format.datetime.joda;

import java.util.Locale;

import org.joda.time.format.DateTimeFormatter;

import org.springframework.format.Printer;

/**
 * 使用Joda {@link DateTimeFormatter}打印Long实例.
 */
public final class MillisecondInstantPrinter implements Printer<Long> {

	private final DateTimeFormatter formatter;


	/**
	 * @param formatter Joda DateTimeFormatter实例
	 */
	public MillisecondInstantPrinter(DateTimeFormatter formatter) {
		this.formatter = formatter;
	}


	@Override
	public String print(Long instant, Locale locale) {
		return JodaTimeContextHolder.getFormatter(this.formatter, locale).print(instant);
	}

}
