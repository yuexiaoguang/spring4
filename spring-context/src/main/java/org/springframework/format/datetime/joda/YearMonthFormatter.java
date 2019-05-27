package org.springframework.format.datetime.joda;

import java.text.ParseException;
import java.util.Locale;

import org.joda.time.YearMonth;

import org.springframework.format.Formatter;

/**
 * Joda-Time {@link YearMonth}的{@link Formatter}实现, 遵循Joda-Time的YearMonth解析规则.
 */
class YearMonthFormatter implements Formatter<YearMonth> {

	@Override
	public YearMonth parse(String text, Locale locale) throws ParseException {
		return YearMonth.parse(text);
	}

	@Override
	public String print(YearMonth object, Locale locale) {
		return object.toString();
	}

}
