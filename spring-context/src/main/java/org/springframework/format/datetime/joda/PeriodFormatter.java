package org.springframework.format.datetime.joda;

import java.text.ParseException;
import java.util.Locale;

import org.joda.time.Period;

import org.springframework.format.Formatter;

/**
 * Joda-Time {@link Period}的{@link Formatter}实现, 遵循Period的Joda-Time解析规则.
 */
class PeriodFormatter implements Formatter<Period> {

	@Override
	public Period parse(String text, Locale locale) throws ParseException {
		return Period.parse(text);
	}

	@Override
	public String print(Period object, Locale locale) {
		return object.toString();
	}

}
