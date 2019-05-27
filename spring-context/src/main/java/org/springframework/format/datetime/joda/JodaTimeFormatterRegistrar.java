package org.springframework.format.datetime.joda;

import java.util.Calendar;
import java.util.Date;
import java.util.EnumMap;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.MonthDay;
import org.joda.time.Period;
import org.joda.time.ReadableInstant;
import org.joda.time.YearMonth;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import org.springframework.format.FormatterRegistrar;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.Parser;
import org.springframework.format.Printer;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.util.ClassUtils;

/**
 * 配置Joda-Time的格式化系统, 以与Spring一起使用.
 *
 * <p><b>NOTE:</b> Spring的Joda-Time支持需要Joda-Time 2.x, 从Spring 4.0开始.
 */
public class JodaTimeFormatterRegistrar implements FormatterRegistrar {

	private enum Type {DATE, TIME, DATE_TIME}


	/**
	 * 严格来说, 这不应该是必要的, 因为我们正式要求JodaTime 2.x.
	 * 但是, 由于Joda-Time格式化器正在自动注册, 因此我们在类路径上遇到Joda-Time 1.x时会进行防御性调整.
	 * 要在Spring 5.0中删除.
	 */
	private static final boolean jodaTime2Available = ClassUtils.isPresent(
			"org.joda.time.YearMonth", JodaTimeFormatterRegistrar.class.getClassLoader());

	/**
	 * 用户定义的格式化器.
	 */
	private final Map<Type, DateTimeFormatter> formatters = new EnumMap<Type, DateTimeFormatter>(Type.class);

	/**
	 * 未指定特定格式化器时使用的工厂.
	 */
	private final Map<Type, DateTimeFormatterFactory> factories;


	public JodaTimeFormatterRegistrar() {
		this.factories = new EnumMap<Type, DateTimeFormatterFactory>(Type.class);
		for (Type type : Type.values()) {
			this.factories.put(type, new DateTimeFormatterFactory());
		}
	}


	/**
	 * 设置是否应将标准ISO格式应用于所有日期/时间类型.
	 * 默认是 "false" (no).
	 * <p>如果设置为 "true", 则会有效地忽略"dateStyle", "timeStyle", "dateTimeStyle"属性.
	 */
	public void setUseIsoFormat(boolean useIsoFormat) {
		this.factories.get(Type.DATE).setIso(useIsoFormat ? ISO.DATE : null);
		this.factories.get(Type.TIME).setIso(useIsoFormat ? ISO.TIME : null);
		this.factories.get(Type.DATE_TIME).setIso(useIsoFormat ? ISO.DATE_TIME : null);
	}

	/**
	 * 设置Joda {@link LocalDate}对象的默认格式样式.
	 * 默认 {@link DateTimeFormat#shortDate()}.
	 */
	public void setDateStyle(String dateStyle) {
		this.factories.get(Type.DATE).setStyle(dateStyle + "-");
	}

	/**
	 * 设置Joda {@link LocalTime}对象的默认格式样式.
	 * 默认是 {@link DateTimeFormat#shortTime()}.
	 */
	public void setTimeStyle(String timeStyle) {
		this.factories.get(Type.TIME).setStyle("-" + timeStyle);
	}

	/**
	 * 设置Joda {@link LocalDateTime}和{@link DateTime}对象, 以及JDK {@link Date}和{@link Calendar}对象的默认格式样式.
	 * 默认是 {@link DateTimeFormat#shortDateTime()}.
	 */
	public void setDateTimeStyle(String dateTimeStyle) {
		this.factories.get(Type.DATE_TIME).setStyle(dateTimeStyle);
	}

	/**
	 * 设置将用于表示日期值的对象的格式化器.
	 * <p>此格式化器将用于{@link LocalDate}类型.
	 * 当指定{@link #setDateStyle(String) dateStyle} 和 {@link #setUseIsoFormat(boolean) useIsoFormat}属性时将被忽略.
	 * 
	 * @param formatter 要使用的格式化器
	 */
	public void setDateFormatter(DateTimeFormatter formatter) {
		this.formatters.put(Type.DATE, formatter);
	}

	/**
	 * 设置将用于表示时间值的对象的格式化器.
	 * <p>此格式化器将用于 {@link LocalTime}类型.
	 * 当指定 {@link #setTimeStyle(String) timeStyle} 和 {@link #setUseIsoFormat(boolean) useIsoFormat}属性时将被忽略.
	 * 
	 * @param formatter 要使用的格式化器
	 */
	public void setTimeFormatter(DateTimeFormatter formatter) {
		this.formatters.put(Type.TIME, formatter);
	}

	/**
	 * 设置将用于表示日期和时间值的对象的格式化器.
	 * <p>此格式化器将用于 {@link LocalDateTime}, {@link ReadableInstant}, {@link Date}, {@link Calendar}类型.
	 * 当指定 {@link #setDateTimeStyle(String) dateTimeStyle} 和 {@link #setUseIsoFormat(boolean) useIsoFormat}属性时将被忽略.
	 * 
	 * @param formatter 要使用的格式化器
	 */
	public void setDateTimeFormatter(DateTimeFormatter formatter) {
		this.formatters.put(Type.DATE_TIME, formatter);
	}


	@Override
	public void registerFormatters(FormatterRegistry registry) {
		JodaTimeConverters.registerConverters(registry);

		DateTimeFormatter dateFormatter = getFormatter(Type.DATE);
		DateTimeFormatter timeFormatter = getFormatter(Type.TIME);
		DateTimeFormatter dateTimeFormatter = getFormatter(Type.DATE_TIME);

		addFormatterForFields(registry,
				new ReadablePartialPrinter(dateFormatter),
				new LocalDateParser(dateFormatter),
				LocalDate.class);

		addFormatterForFields(registry,
				new ReadablePartialPrinter(timeFormatter),
				new LocalTimeParser(timeFormatter),
				LocalTime.class);

		addFormatterForFields(registry,
				new ReadablePartialPrinter(dateTimeFormatter),
				new LocalDateTimeParser(dateTimeFormatter),
				LocalDateTime.class);

		addFormatterForFields(registry,
				new ReadableInstantPrinter(dateTimeFormatter),
				new DateTimeParser(dateTimeFormatter),
				ReadableInstant.class);

		// 为了保持向后兼容性, 仅在指定了用户定义的格式化器时注册Date/Calendar类型 (see SPR-10105)
		if (this.formatters.containsKey(Type.DATE_TIME)) {
			addFormatterForFields(registry,
					new ReadableInstantPrinter(dateTimeFormatter),
					new DateTimeParser(dateTimeFormatter),
					Date.class, Calendar.class);
		}

		registry.addFormatterForFieldType(Period.class, new PeriodFormatter());
		registry.addFormatterForFieldType(Duration.class, new DurationFormatter());
		if (jodaTime2Available) {
			JodaTime2Delegate.registerAdditionalFormatters(registry);
		}

		registry.addFormatterForFieldAnnotation(new JodaDateTimeFormatAnnotationFormatterFactory());
	}

	private DateTimeFormatter getFormatter(Type type) {
		DateTimeFormatter formatter = this.formatters.get(type);
		if (formatter != null) {
			return formatter;
		}
		DateTimeFormatter fallbackFormatter = getFallbackFormatter(type);
		return this.factories.get(type).createDateTimeFormatter(fallbackFormatter);
	}

	private DateTimeFormatter getFallbackFormatter(Type type) {
		switch (type) {
			case DATE: return DateTimeFormat.shortDate();
			case TIME: return DateTimeFormat.shortTime();
			default: return DateTimeFormat.shortDateTime();
		}
	}

	private void addFormatterForFields(FormatterRegistry registry, Printer<?> printer,
			Parser<?> parser, Class<?>... fieldTypes) {

		for (Class<?> fieldType : fieldTypes) {
			registry.addFormatterForFieldType(fieldType, printer, parser);
		}
	}


	/**
	 * 内部类, 以避免对Joda-Time 2.x的硬依赖.
	 */
	private static class JodaTime2Delegate {

		public static void registerAdditionalFormatters(FormatterRegistry registry) {
			registry.addFormatterForFieldType(YearMonth.class, new YearMonthFormatter());
			registry.addFormatterForFieldType(MonthDay.class, new MonthDayFormatter());
		}
	}

}
