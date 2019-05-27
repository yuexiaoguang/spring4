package org.springframework.format.datetime.standard;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.context.support.EmbeddedValueResolutionSupport;
import org.springframework.format.AnnotationFormatterFactory;
import org.springframework.format.Parser;
import org.springframework.format.Printer;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.UsesJava8;

/**
 * 使用JDK 8中的JSR-310 <code>java.time</code>包格式化带{@link DateTimeFormat}注解的字段.
 */
@UsesJava8
public class Jsr310DateTimeFormatAnnotationFormatterFactory extends EmbeddedValueResolutionSupport
		implements AnnotationFormatterFactory<DateTimeFormat> {

	private static final Set<Class<?>> FIELD_TYPES;

	static {
		// 创建可以使用@DateTimeFormat注解的字段类型集合.
		Set<Class<?>> fieldTypes = new HashSet<Class<?>>(8);
		fieldTypes.add(LocalDate.class);
		fieldTypes.add(LocalTime.class);
		fieldTypes.add(LocalDateTime.class);
		fieldTypes.add(ZonedDateTime.class);
		fieldTypes.add(OffsetDateTime.class);
		fieldTypes.add(OffsetTime.class);
		FIELD_TYPES = Collections.unmodifiableSet(fieldTypes);
	}


	@Override
	public final Set<Class<?>> getFieldTypes() {
		return FIELD_TYPES;
	}

	@Override
	public Printer<?> getPrinter(DateTimeFormat annotation, Class<?> fieldType) {
		DateTimeFormatter formatter = getFormatter(annotation, fieldType);

		// 用于打印的高效ISO_LOCAL_* 变体, 因为它们的速度是原来的两倍...
		if (formatter == DateTimeFormatter.ISO_DATE) {
			if (isLocal(fieldType)) {
				formatter = DateTimeFormatter.ISO_LOCAL_DATE;
			}
		}
		else if (formatter == DateTimeFormatter.ISO_TIME) {
			if (isLocal(fieldType)) {
				formatter = DateTimeFormatter.ISO_LOCAL_TIME;
			}
		}
		else if (formatter == DateTimeFormatter.ISO_DATE_TIME) {
			if (isLocal(fieldType)) {
				formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
			}
		}

		return new TemporalAccessorPrinter(formatter);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Parser<?> getParser(DateTimeFormat annotation, Class<?> fieldType) {
		DateTimeFormatter formatter = getFormatter(annotation, fieldType);
		return new TemporalAccessorParser((Class<? extends TemporalAccessor>) fieldType, formatter);
	}

	/**
	 * 用于创建{@link DateTimeFormatter}的工厂方法.
	 * 
	 * @param annotation 字段的格式注解
	 * @param fieldType 声明的字段类型
	 * 
	 * @return {@link DateTimeFormatter}实例
	 */
	protected DateTimeFormatter getFormatter(DateTimeFormat annotation, Class<?> fieldType) {
		DateTimeFormatterFactory factory = new DateTimeFormatterFactory();
		factory.setStylePattern(resolveEmbeddedValue(annotation.style()));
		factory.setIso(annotation.iso());
		factory.setPattern(resolveEmbeddedValue(annotation.pattern()));
		return factory.createDateTimeFormatter();
	}

	private boolean isLocal(Class<?> fieldType) {
		return fieldType.getSimpleName().startsWith("Local");
	}

}
