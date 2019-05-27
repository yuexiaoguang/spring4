package org.springframework.format.datetime.joda;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.ReadableInstant;
import org.joda.time.ReadablePartial;
import org.joda.time.format.DateTimeFormatter;

import org.springframework.context.support.EmbeddedValueResolutionSupport;
import org.springframework.format.AnnotationFormatterFactory;
import org.springframework.format.Parser;
import org.springframework.format.Printer;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * 使用Joda-Time格式化使用{@link DateTimeFormat}注解的字段.
 *
 * <p><b>NOTE:</b> Spring的Joda-Time支持需要 Joda-Time 2.x, 从Spring 4.0开始.
 */
public class JodaDateTimeFormatAnnotationFormatterFactory extends EmbeddedValueResolutionSupport
		implements AnnotationFormatterFactory<DateTimeFormat> {

	private static final Set<Class<?>> FIELD_TYPES;

	static {
		// 创建可以使用@DateTimeFormat注解的字段类型集合.
		// Note: 3个ReadablePartial具体类型是显式注册的, 因为每种类型都存在addFormatterForFieldType规则
		// (如果不这样做, LocalDate, LocalTime和LocalDateTime的默认byType规则将优先于注解规则, 这不是我们想要的)
		Set<Class<?>> fieldTypes = new HashSet<Class<?>>(8);
		fieldTypes.add(ReadableInstant.class);
		fieldTypes.add(LocalDate.class);
		fieldTypes.add(LocalTime.class);
		fieldTypes.add(LocalDateTime.class);
		fieldTypes.add(Date.class);
		fieldTypes.add(Calendar.class);
		fieldTypes.add(Long.class);
		FIELD_TYPES = Collections.unmodifiableSet(fieldTypes);
	}


	@Override
	public final Set<Class<?>> getFieldTypes() {
		return FIELD_TYPES;
	}

	@Override
	public Printer<?> getPrinter(DateTimeFormat annotation, Class<?> fieldType) {
		DateTimeFormatter formatter = getFormatter(annotation, fieldType);
		if (ReadablePartial.class.isAssignableFrom(fieldType)) {
			return new ReadablePartialPrinter(formatter);
		}
		else if (ReadableInstant.class.isAssignableFrom(fieldType) || Calendar.class.isAssignableFrom(fieldType)) {
			// assumes Calendar->ReadableInstant converter is registered
			return new ReadableInstantPrinter(formatter);
		}
		else {
			// assumes Date->Long converter is registered
			return new MillisecondInstantPrinter(formatter);
		}
	}

	@Override
	public Parser<?> getParser(DateTimeFormat annotation, Class<?> fieldType) {
		if (LocalDate.class == fieldType) {
			return new LocalDateParser(getFormatter(annotation, fieldType));
		}
		else if (LocalTime.class == fieldType) {
			return new LocalTimeParser(getFormatter(annotation, fieldType));
		}
		else if (LocalDateTime.class == fieldType) {
			return new LocalDateTimeParser(getFormatter(annotation, fieldType));
		}
		else {
			return new DateTimeParser(getFormatter(annotation, fieldType));
		}
	}

	/**
	 * 用于创建{@link DateTimeFormatter}的工厂方法.
	 * 
	 * @param annotation 字段的格式注解
	 * @param fieldType 字段的类型
	 * 
	 * @return a {@link DateTimeFormatter}实例
	 */
	protected DateTimeFormatter getFormatter(DateTimeFormat annotation, Class<?> fieldType) {
		DateTimeFormatterFactory factory = new DateTimeFormatterFactory();
		factory.setStyle(resolveEmbeddedValue(annotation.style()));
		factory.setIso(annotation.iso());
		factory.setPattern(resolveEmbeddedValue(annotation.pattern()));
		return factory.createDateTimeFormatter();
	}

}
