package org.springframework.core.convert.support;

import java.util.Set;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.core.convert.converter.GenericConverter;

/**
 * 常见{@link org.springframework.core.convert.ConversionService}配置的工厂.
 */
public abstract class ConversionServiceFactory {

	/**
	 * 使用给定的目标ConverterRegistry注册给定的Converter对象.
	 * 
	 * @param converters 转换器对象: 实现{@link Converter}, {@link ConverterFactory}, 或{@link GenericConverter}
	 * @param registry 目标注册表
	 */
	public static void registerConverters(Set<?> converters, ConverterRegistry registry) {
		if (converters != null) {
			for (Object converter : converters) {
				if (converter instanceof GenericConverter) {
					registry.addConverter((GenericConverter) converter);
				}
				else if (converter instanceof Converter<?, ?>) {
					registry.addConverter((Converter<?, ?>) converter);
				}
				else if (converter instanceof ConverterFactory<?, ?>) {
					registry.addConverterFactory((ConverterFactory<?, ?>) converter);
				}
				else {
					throw new IllegalArgumentException("Each converter object must implement one of the " +
							"Converter, ConverterFactory, or GenericConverter interfaces");
				}
			}
		}
	}

}
