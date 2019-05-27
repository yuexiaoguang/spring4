package org.springframework.format.datetime.standard;

import java.text.ParseException;
import java.time.Duration;
import java.util.Locale;

import org.springframework.format.Formatter;
import org.springframework.lang.UsesJava8;

/**
 * JSR-310 {@link Duration}的{@link Formatter}实现, 遵循JSR-310的Duration解析规则.
 */
@UsesJava8
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
