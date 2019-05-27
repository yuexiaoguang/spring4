package org.springframework.format.support;

import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.datetime.DateFormatterRegistrar;
import org.springframework.format.datetime.joda.JodaTimeFormatterRegistrar;
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar;
import org.springframework.format.number.money.CurrencyUnitFormatter;
import org.springframework.format.number.money.Jsr354NumberFormatAnnotationFormatterFactory;
import org.springframework.format.number.money.MonetaryAmountFormatter;
import org.springframework.format.number.NumberFormatAnnotationFormatterFactory;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringValueResolver;

/**
 * 默认情况下配置{@link FormattingConversionService}的专门化, 使用适用于大多数应用程序的转换器和格式化器.
 *
 * <p>设计用于直接实例化, 但也公开静态 {@link #addDefaultFormatters} 实用程序方法,
 * 以便{@code FormatterRegistry}实例进行临时使用,
 * 就像{@code DefaultConversionService}公开自己的 {@link DefaultConversionService#addDefaultConverters addDefaultConverters}方法一样.
 *
 * <p>自动注册 JSR-354 Money & Currency, JSR-310 Date-Time 和/或 Joda-Time的格式化器, 具体取决于类路径上是否存在相应的API.
 */
public class DefaultFormattingConversionService extends FormattingConversionService {

	private static final boolean jsr354Present = ClassUtils.isPresent(
			"javax.money.MonetaryAmount", DefaultFormattingConversionService.class.getClassLoader());

	private static final boolean jsr310Present = ClassUtils.isPresent(
			"java.time.LocalDate", DefaultFormattingConversionService.class.getClassLoader());

	private static final boolean jodaTimePresent = ClassUtils.isPresent(
			"org.joda.time.LocalDate", DefaultFormattingConversionService.class.getClassLoader());


	public DefaultFormattingConversionService() {
		this(null, true);
	}

	/**
	 * @param registerDefaultFormatters 是否注册默认格式化器
	 */
	public DefaultFormattingConversionService(boolean registerDefaultFormatters) {
		this(null, registerDefaultFormatters);
	}

	/**
	 * @param embeddedValueResolver 委托给{@link #setEmbeddedValueResolver(StringValueResolver)},
	 * 在调用 {@link #addDefaultFormatters}之前.
	 * @param registerDefaultFormatters 是否注册默认格式化器
	 */
	public DefaultFormattingConversionService(StringValueResolver embeddedValueResolver, boolean registerDefaultFormatters) {
		setEmbeddedValueResolver(embeddedValueResolver);
		DefaultConversionService.addDefaultConverters(this);
		if (registerDefaultFormatters) {
			addDefaultFormatters(this);
		}
	}


	/**
	 * 添加适合大多数环境的格式化器:
	 * 包括数字格式化器, JSR-354 Money & Currency格式化器, JSR-310 Date-Time 和/或 Joda-Time格式化器,
	 * 取决于类路径上是否存在相应的API.
	 * 
	 * @param formatterRegistry 用于注册默认格式化器的服务
	 */
	public static void addDefaultFormatters(FormatterRegistry formatterRegistry) {
		// 数值的默认处理
		formatterRegistry.addFormatterForFieldAnnotation(new NumberFormatAnnotationFormatterFactory());

		// 货币值的默认处理
		if (jsr354Present) {
			formatterRegistry.addFormatter(new CurrencyUnitFormatter());
			formatterRegistry.addFormatter(new MonetaryAmountFormatter());
			formatterRegistry.addFormatterForFieldAnnotation(new Jsr354NumberFormatAnnotationFormatterFactory());
		}

		// 日期时间值的默认处理
		if (jsr310Present) {
			// 只处理JSR-310特定的日期和时间类型
			new DateTimeFormatterRegistrar().registerFormatters(formatterRegistry);
		}
		if (jodaTimePresent) {
			// 处理Joda特定类型以及 Date, Calendar, Long
			new JodaTimeFormatterRegistrar().registerFormatters(formatterRegistry);
		}
		else {
			// 基于DateFormat的 Date, Calendar, Long 转换器
			new DateFormatterRegistrar().registerFormatters(formatterRegistry);
		}
	}

}
