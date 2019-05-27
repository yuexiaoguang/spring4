package org.springframework.core.convert.support;

import java.nio.charset.Charset;
import java.util.Currency;
import java.util.Locale;
import java.util.UUID;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.util.ClassUtils;

/**
 * 默认情况下配置的{@link GenericConversionService}的专门化, 使用适用于大多数环境的转换器.
 *
 * <p>设计用于直接实例化, 但也公开静态的{@link #addDefaultConverters(ConverterRegistry)}工具方法,
 * 以便针对任何{@code ConverterRegistry}实例进行临时使用.
 */
public class DefaultConversionService extends GenericConversionService {

	/** Java 8's java.util.Optional class available? */
	private static final boolean javaUtilOptionalClassAvailable =
			ClassUtils.isPresent("java.util.Optional", DefaultConversionService.class.getClassLoader());

	/** Java 8's java.time package available? */
	private static final boolean jsr310Available =
			ClassUtils.isPresent("java.time.ZoneId", DefaultConversionService.class.getClassLoader());

	/** Java 8's java.util.stream.Stream class available? */
	private static final boolean streamAvailable = ClassUtils.isPresent(
			"java.util.stream.Stream", DefaultConversionService.class.getClassLoader());

	private static volatile DefaultConversionService sharedInstance;


	/**
	 * 使用一组
	 * {@linkplain DefaultConversionService#addDefaultConverters(ConverterRegistry) 默认转换器}.
	 */
	public DefaultConversionService() {
		addDefaultConverters(this);
	}


	/**
	 * 返回一个共享的默认{@code ConversionService}实例, 一旦需要就延迟构建它.
	 * <p><b>NOTE:</b> 强烈建议构建单独的{@code ConversionService}实例以进行自定义.
	 * 这个访问器只是作为代码路径的后备, 它需要简单的类型强制,
	 * 但不能以任何其他方式访问寿命更长的{@code ConversionService}实例.
	 * 
	 * @return 共享的{@code ConversionService}实例 (never {@code null})
	 */
	public static ConversionService getSharedInstance() {
		if (sharedInstance == null) {
			synchronized (DefaultConversionService.class) {
				if (sharedInstance == null) {
					sharedInstance = new DefaultConversionService();
				}
			}
		}
		return sharedInstance;
	}

	/**
	 * 添加适合大多数环境的转换器.
	 * 
	 * @param converterRegistry 要添加到的转换器的注册表
	 * (必须也可以转换为ConversionService, e.g. 是一个{@link ConfigurableConversionService})
	 * 
	 * @throws ClassCastException 如果给定的ConverterRegistry无法转换为ConversionService
	 */
	public static void addDefaultConverters(ConverterRegistry converterRegistry) {
		addScalarConverters(converterRegistry);
		addCollectionConverters(converterRegistry);

		converterRegistry.addConverter(new ByteBufferConverter((ConversionService) converterRegistry));
		if (jsr310Available) {
			Jsr310ConverterRegistrar.registerJsr310Converters(converterRegistry);
		}

		converterRegistry.addConverter(new ObjectToObjectConverter());
		converterRegistry.addConverter(new IdToEntityConverter((ConversionService) converterRegistry));
		converterRegistry.addConverter(new FallbackObjectToStringConverter());
		if (javaUtilOptionalClassAvailable) {
			converterRegistry.addConverter(new ObjectToOptionalConverter((ConversionService) converterRegistry));
		}
	}

	/**
	 * 添加通用集合转换器.
	 * 
	 * @param converterRegistry 要添加到的转换器的注册表
	 * (必须也可以转换为ConversionService, e.g. 是一个{@link ConfigurableConversionService})
	 * 
	 * @throws ClassCastException 如果给定的ConverterRegistry无法转换为ConversionService
	 */
	public static void addCollectionConverters(ConverterRegistry converterRegistry) {
		ConversionService conversionService = (ConversionService) converterRegistry;

		converterRegistry.addConverter(new ArrayToCollectionConverter(conversionService));
		converterRegistry.addConverter(new CollectionToArrayConverter(conversionService));

		converterRegistry.addConverter(new ArrayToArrayConverter(conversionService));
		converterRegistry.addConverter(new CollectionToCollectionConverter(conversionService));
		converterRegistry.addConverter(new MapToMapConverter(conversionService));

		converterRegistry.addConverter(new ArrayToStringConverter(conversionService));
		converterRegistry.addConverter(new StringToArrayConverter(conversionService));

		converterRegistry.addConverter(new ArrayToObjectConverter(conversionService));
		converterRegistry.addConverter(new ObjectToArrayConverter(conversionService));

		converterRegistry.addConverter(new CollectionToStringConverter(conversionService));
		converterRegistry.addConverter(new StringToCollectionConverter(conversionService));

		converterRegistry.addConverter(new CollectionToObjectConverter(conversionService));
		converterRegistry.addConverter(new ObjectToCollectionConverter(conversionService));

		if (streamAvailable) {
			converterRegistry.addConverter(new StreamConverter(conversionService));
		}
	}

	private static void addScalarConverters(ConverterRegistry converterRegistry) {
		converterRegistry.addConverterFactory(new NumberToNumberConverterFactory());

		converterRegistry.addConverterFactory(new StringToNumberConverterFactory());
		converterRegistry.addConverter(Number.class, String.class, new ObjectToStringConverter());

		converterRegistry.addConverter(new StringToCharacterConverter());
		converterRegistry.addConverter(Character.class, String.class, new ObjectToStringConverter());

		converterRegistry.addConverter(new NumberToCharacterConverter());
		converterRegistry.addConverterFactory(new CharacterToNumberFactory());

		converterRegistry.addConverter(new StringToBooleanConverter());
		converterRegistry.addConverter(Boolean.class, String.class, new ObjectToStringConverter());

		converterRegistry.addConverterFactory(new StringToEnumConverterFactory());
		converterRegistry.addConverter(new EnumToStringConverter((ConversionService) converterRegistry));

		converterRegistry.addConverterFactory(new IntegerToEnumConverterFactory());
		converterRegistry.addConverter(new EnumToIntegerConverter((ConversionService) converterRegistry));

		converterRegistry.addConverter(new StringToLocaleConverter());
		converterRegistry.addConverter(Locale.class, String.class, new ObjectToStringConverter());

		converterRegistry.addConverter(new StringToCharsetConverter());
		converterRegistry.addConverter(Charset.class, String.class, new ObjectToStringConverter());

		converterRegistry.addConverter(new StringToCurrencyConverter());
		converterRegistry.addConverter(Currency.class, String.class, new ObjectToStringConverter());

		converterRegistry.addConverter(new StringToPropertiesConverter());
		converterRegistry.addConverter(new PropertiesToStringConverter());

		converterRegistry.addConverter(new StringToUUIDConverter());
		converterRegistry.addConverter(UUID.class, String.class, new ObjectToStringConverter());
	}


	/**
	 * 内部类, 以避免对Java 8的{@code java.time}包进行硬编码依赖.
	 */
	private static final class Jsr310ConverterRegistrar {

		public static void registerJsr310Converters(ConverterRegistry converterRegistry) {
			converterRegistry.addConverter(new StringToTimeZoneConverter());
			converterRegistry.addConverter(new ZoneIdToTimeZoneConverter());
			converterRegistry.addConverter(new ZonedDateTimeToCalendarConverter());
		}
	}
}
