package org.springframework.format.datetime.standard;

import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;

import org.springframework.format.Printer;
import org.springframework.lang.UsesJava8;

/**
 * JSR-310 {@link java.time.temporal.TemporalAccessor}的{@link Printer}实现, 使用{@link java.time.format.DateTimeFormatter})
 * (上下文一个, 如果可用的话).
 */
@UsesJava8
public final class TemporalAccessorPrinter implements Printer<TemporalAccessor> {

	private final DateTimeFormatter formatter;


	/**
	 * @param formatter 基础DateTimeFormatter实例
	 */
	public TemporalAccessorPrinter(DateTimeFormatter formatter) {
		this.formatter = formatter;
	}


	@Override
	public String print(TemporalAccessor partial, Locale locale) {
		return DateTimeContextHolder.getFormatter(this.formatter, locale).format(partial);
	}

}
