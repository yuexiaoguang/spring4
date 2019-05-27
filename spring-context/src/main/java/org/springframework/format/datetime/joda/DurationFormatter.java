package org.springframework.format.datetime.joda;

import java.text.ParseException;
import java.util.Locale;

import org.joda.time.Duration;

import org.springframework.format.Formatter;

/**
 * Joda-Time {@link Duration}的{@link Formatter}实现, 遵循Joda-Time的Duration解析规则.
 */
class DurationFormatter implements Formatter<Duration> {

	@Override
	public Duration parse(String text, Locale locale) throws ParseException {
		return Duration.parse(text);
	}

	@Override
	public String print(Duration object, Locale locale) {
		return object.toString();
	}

}
