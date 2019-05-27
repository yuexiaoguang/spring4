package org.springframework.format.datetime.standard;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;

import org.springframework.format.Parser;
import org.springframework.lang.UsesJava8;

/**
 * JSR-310 {@link java.time.temporal.TemporalAccessor}的{@link Parser}实现,
 * 使用{@link java.time.format.DateTimeFormatter}) (the contextual one, if available).
 */
@UsesJava8
public final class TemporalAccessorParser implements Parser<TemporalAccessor> {

	private final Class<? extends TemporalAccessor> temporalAccessorType;

	private final DateTimeFormatter formatter;


	/**
	 * @param temporalAccessorType 特定的TemporalAccessor类
	 * (LocalDate, LocalTime, LocalDateTime, ZonedDateTime, OffsetDateTime, OffsetTime)
	 * @param formatter 基础DateTimeFormatter实例
	 */
	public TemporalAccessorParser(Class<? extends TemporalAccessor> temporalAccessorType, DateTimeFormatter formatter) {
		this.temporalAccessorType = temporalAccessorType;
		this.formatter = formatter;
	}


	@Override
	public TemporalAccessor parse(String text, Locale locale) throws ParseException {
		DateTimeFormatter formatterToUse = DateTimeContextHolder.getFormatter(this.formatter, locale);
		if (LocalDate.class == this.temporalAccessorType) {
			return LocalDate.parse(text, formatterToUse);
		}
		else if (LocalTime.class == this.temporalAccessorType) {
			return LocalTime.parse(text, formatterToUse);
		}
		else if (LocalDateTime.class == this.temporalAccessorType) {
			return LocalDateTime.parse(text, formatterToUse);
		}
		else if (ZonedDateTime.class == this.temporalAccessorType) {
			return ZonedDateTime.parse(text, formatterToUse);
		}
		else if (OffsetDateTime.class == this.temporalAccessorType) {
			return OffsetDateTime.parse(text, formatterToUse);
		}
		else if (OffsetTime.class == this.temporalAccessorType) {
			return OffsetTime.parse(text, formatterToUse);
		}
		else {
			throw new IllegalStateException("Unsupported TemporalAccessor type: " + this.temporalAccessorType);
		}
	}

}
