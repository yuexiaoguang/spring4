package org.springframework.core.convert.support;

import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.UsesJava8;

/**
 * 从Java 8的{@link java.time.ZonedDateTime}到{@link java.util.Calendar}的简单转换器.
 *
 * <p>请注意, Spring的默认ConversionService设置理解JSR-310 {@code java.time}包一直使用的'from'/'to'约定.
 * 该约定在{@link ObjectToObjectConverter}中反射实现, 而不是在特定的JSR-310转换器中实现.
 * 它还包括{@link java.util.GregorianCalendar#toZonedDateTime()},
 * 以及{@link java.util.Date#from(java.time.Instant)}和 {@link java.util.Date#toInstant()}.
 */
@UsesJava8
final class ZonedDateTimeToCalendarConverter implements Converter<ZonedDateTime, Calendar> {

	@Override
	public Calendar convert(ZonedDateTime source) {
		return GregorianCalendar.from(source);
	}

}
