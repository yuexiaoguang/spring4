package org.springframework.format.datetime.joda;

import java.text.ParseException;
import java.util.Locale;

import org.joda.time.MonthDay;

import org.springframework.format.Formatter;

/**
 * Joda-Time {@link MonthDay}的{@link Formatter}实现, 遵循Joda-Time的MonthDay解析规则.
 */
class MonthDayFormatter implements Formatter<MonthDay> {

	@Override
	public MonthDay parse(String text, Locale locale) throws ParseException {
		return MonthDay.parse(text);
	}

	@Override
	public String print(MonthDay object, Locale locale) {
		return object.toString();
	}

}
