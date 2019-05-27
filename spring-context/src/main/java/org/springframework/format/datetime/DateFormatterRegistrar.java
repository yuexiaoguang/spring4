package org.springframework.format.datetime;

import java.util.Calendar;
import java.util.Date;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.format.FormatterRegistrar;
import org.springframework.format.FormatterRegistry;
import org.springframework.util.Assert;

/**
 * 配置与Spring一起使用的基本日期格式, 主要用于{@link org.springframework.format.annotation.DateTimeFormat}声明.
 * 适用于{@link Date}, {@link Calendar}, {@code long}类型的字段.
 *
 * <p>设计用于直接实例化, 但也公开静态{@link #addDateConverters(ConverterRegistry)}实用程序方法,
 * 以便针对任何{@code ConverterRegistry}实例进行临时使用.
 */
public class DateFormatterRegistrar implements FormatterRegistrar {

	private DateFormatter dateFormatter;


	/**
	 * 设置要注册的全局日期格式化器.
	 * <p>如果未指定, 则不会注册未注解的 {@link Date} 和 {@link Calendar}字段的常规格式化器.
	 */
	public void setFormatter(DateFormatter dateFormatter) {
		Assert.notNull(dateFormatter, "DateFormatter must not be null");
		this.dateFormatter = dateFormatter;
	}


	@Override
	public void registerFormatters(FormatterRegistry registry) {
		addDateConverters(registry);
		registry.addFormatterForFieldAnnotation(new DateTimeFormatAnnotationFormatterFactory());

		// 为了保持兼容性, 只在指定用户定义的格式化器时注册Date/Calendar类型 (see SPR-10105)
		if (this.dateFormatter != null) {
			registry.addFormatter(this.dateFormatter);
			registry.addFormatterForFieldType(Calendar.class, this.dateFormatter);
		}
	}

	/**
	 * 将日期转换器添加到指定的注册表.
	 * 
	 * @param converterRegistry 要添加到的转换器的注册表
	 */
	public static void addDateConverters(ConverterRegistry converterRegistry) {
		converterRegistry.addConverter(new DateToLongConverter());
		converterRegistry.addConverter(new DateToCalendarConverter());
		converterRegistry.addConverter(new CalendarToDateConverter());
		converterRegistry.addConverter(new CalendarToLongConverter());
		converterRegistry.addConverter(new LongToDateConverter());
		converterRegistry.addConverter(new LongToCalendarConverter());
	}


	private static class DateToLongConverter implements Converter<Date, Long> {

		@Override
		public Long convert(Date source) {
			return source.getTime();
		}
	}


	private static class DateToCalendarConverter implements Converter<Date, Calendar> {

		@Override
		public Calendar convert(Date source) {
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(source);
			return calendar;
		}
	}


	private static class CalendarToDateConverter implements Converter<Calendar, Date> {

		@Override
		public Date convert(Calendar source) {
			return source.getTime();
		}
	}


	private static class CalendarToLongConverter implements Converter<Calendar, Long> {

		@Override
		public Long convert(Calendar source) {
			return source.getTimeInMillis();
		}
	}


	private static class LongToDateConverter implements Converter<Long, Date> {

		@Override
		public Date convert(Long source) {
			return new Date(source);
		}
	}


	private static class LongToCalendarConverter implements Converter<Long, Calendar> {

		@Override
		public Calendar convert(Long source) {
			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(source);
			return calendar;
		}
	}

}
