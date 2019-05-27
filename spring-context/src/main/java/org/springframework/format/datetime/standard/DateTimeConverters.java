package org.springframework.format.datetime.standard;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.format.datetime.DateFormatterRegistrar;
import org.springframework.lang.UsesJava8;

/**
 * 安装将JSR-310支持集成到Spring的字段格式化系统, 所需的低级类型转换器.
 *
 * <p>Note: {@link DateTimeFormatterRegistrar}安装这些转换器, 但不依赖于它们的格式化器.
 * 它们只是在不同的JSR-310值类型之间, 以及{@link java.util.Calendar}和JSR-310值类型之间的自定义转换场景中注册.
 */
@UsesJava8
final class DateTimeConverters {

	/**
	 * 将转换器安装到转换器注册表中.
	 * 
	 * @param registry 转换器注册表
	 */
	public static void registerConverters(ConverterRegistry registry) {
		DateFormatterRegistrar.addDateConverters(registry);

		registry.addConverter(new LocalDateTimeToLocalDateConverter());
		registry.addConverter(new LocalDateTimeToLocalTimeConverter());
		registry.addConverter(new ZonedDateTimeToLocalDateConverter());
		registry.addConverter(new ZonedDateTimeToLocalTimeConverter());
		registry.addConverter(new ZonedDateTimeToLocalDateTimeConverter());
		registry.addConverter(new ZonedDateTimeToOffsetDateTimeConverter());
		registry.addConverter(new ZonedDateTimeToInstantConverter());
		registry.addConverter(new OffsetDateTimeToLocalDateConverter());
		registry.addConverter(new OffsetDateTimeToLocalTimeConverter());
		registry.addConverter(new OffsetDateTimeToLocalDateTimeConverter());
		registry.addConverter(new OffsetDateTimeToZonedDateTimeConverter());
		registry.addConverter(new OffsetDateTimeToInstantConverter());
		registry.addConverter(new CalendarToZonedDateTimeConverter());
		registry.addConverter(new CalendarToOffsetDateTimeConverter());
		registry.addConverter(new CalendarToLocalDateConverter());
		registry.addConverter(new CalendarToLocalTimeConverter());
		registry.addConverter(new CalendarToLocalDateTimeConverter());
		registry.addConverter(new CalendarToInstantConverter());
		registry.addConverter(new LongToInstantConverter());
		registry.addConverter(new InstantToLongConverter());
	}

	private static ZonedDateTime calendarToZonedDateTime(Calendar source) {
		if (source instanceof GregorianCalendar) {
			return ((GregorianCalendar) source).toZonedDateTime();
		}
		else {
			return ZonedDateTime.ofInstant(Instant.ofEpochMilli(source.getTimeInMillis()),
					source.getTimeZone().toZoneId());
		}
	}


	@UsesJava8
	private static class LocalDateTimeToLocalDateConverter implements Converter<LocalDateTime, LocalDate> {

		@Override
		public LocalDate convert(LocalDateTime source) {
			return source.toLocalDate();
		}
	}


	@UsesJava8
	private static class LocalDateTimeToLocalTimeConverter implements Converter<LocalDateTime, LocalTime> {

		@Override
		public LocalTime convert(LocalDateTime source) {
			return source.toLocalTime();
		}
	}


	@UsesJava8
	private static class ZonedDateTimeToLocalDateConverter implements Converter<ZonedDateTime, LocalDate> {

		@Override
		public LocalDate convert(ZonedDateTime source) {
			return source.toLocalDate();
		}
	}


	@UsesJava8
	private static class ZonedDateTimeToLocalTimeConverter implements Converter<ZonedDateTime, LocalTime> {

		@Override
		public LocalTime convert(ZonedDateTime source) {
			return source.toLocalTime();
		}
	}


	@UsesJava8
	private static class ZonedDateTimeToLocalDateTimeConverter implements Converter<ZonedDateTime, LocalDateTime> {

		@Override
		public LocalDateTime convert(ZonedDateTime source) {
			return source.toLocalDateTime();
		}
	}

	@UsesJava8
	private static class ZonedDateTimeToOffsetDateTimeConverter implements Converter<ZonedDateTime, OffsetDateTime> {

		@Override
		public OffsetDateTime convert(ZonedDateTime source) {
			return source.toOffsetDateTime();
		}
	}


	@UsesJava8
	private static class ZonedDateTimeToInstantConverter implements Converter<ZonedDateTime, Instant> {

		@Override
		public Instant convert(ZonedDateTime source) {
			// 为了从-source 1.6调用Java 8默认方法, 必须显式转换为接口
			return ((ChronoZonedDateTime) source).toInstant();
		}
	}


	@UsesJava8
	private static class OffsetDateTimeToLocalDateConverter implements Converter<OffsetDateTime, LocalDate> {

		@Override
		public LocalDate convert(OffsetDateTime source) {
			return source.toLocalDate();
		}
	}


	@UsesJava8
	private static class OffsetDateTimeToLocalTimeConverter implements Converter<OffsetDateTime, LocalTime> {

		@Override
		public LocalTime convert(OffsetDateTime source) {
			return source.toLocalTime();
		}
	}


	@UsesJava8
	private static class OffsetDateTimeToLocalDateTimeConverter implements Converter<OffsetDateTime, LocalDateTime> {

		@Override
		public LocalDateTime convert(OffsetDateTime source) {
			return source.toLocalDateTime();
		}
	}


	@UsesJava8
	private static class OffsetDateTimeToZonedDateTimeConverter implements Converter<OffsetDateTime, ZonedDateTime> {

		@Override
		public ZonedDateTime convert(OffsetDateTime source) {
			return source.toZonedDateTime();
		}
	}


	@UsesJava8
	private static class OffsetDateTimeToInstantConverter implements Converter<OffsetDateTime, Instant> {

		@Override
		public Instant convert(OffsetDateTime source) {
			return source.toInstant();
		}
	}


	@UsesJava8
	private static class CalendarToZonedDateTimeConverter implements Converter<Calendar, ZonedDateTime> {

		@Override
		public ZonedDateTime convert(Calendar source) {
			return calendarToZonedDateTime(source);
		}
	}


	@UsesJava8
	private static class CalendarToOffsetDateTimeConverter implements Converter<Calendar, OffsetDateTime> {

		@Override
		public OffsetDateTime convert(Calendar source) {
			return calendarToZonedDateTime(source).toOffsetDateTime();
		}
	}


	@UsesJava8
	private static class CalendarToLocalDateConverter implements Converter<Calendar, LocalDate> {

		@Override
		public LocalDate convert(Calendar source) {
			return calendarToZonedDateTime(source).toLocalDate();
		}
	}


	@UsesJava8
	private static class CalendarToLocalTimeConverter implements Converter<Calendar, LocalTime> {

		@Override
		public LocalTime convert(Calendar source) {
			return calendarToZonedDateTime(source).toLocalTime();
		}
	}


	@UsesJava8
	private static class CalendarToLocalDateTimeConverter implements Converter<Calendar, LocalDateTime> {

		@Override
		public LocalDateTime convert(Calendar source) {
			return calendarToZonedDateTime(source).toLocalDateTime();
		}
	}


	@UsesJava8
	private static class CalendarToInstantConverter implements Converter<Calendar, Instant> {

		@Override
		public Instant convert(Calendar source) {
			// 为了从-source 1.6调用Java 8默认方法, 必须显式转换为接口
			return ((ChronoZonedDateTime) calendarToZonedDateTime(source)).toInstant();
		}
	}


	@UsesJava8
	private static class LongToInstantConverter implements Converter<Long, Instant> {

		@Override
		public Instant convert(Long source) {
			return Instant.ofEpochMilli(source);
		}
	}


	@UsesJava8
	private static class InstantToLongConverter implements Converter<Instant, Long> {

		@Override
		public Long convert(Instant source) {
			return source.toEpochMilli();
		}
	}

}
