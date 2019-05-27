package org.springframework.format;

import org.springframework.core.convert.converter.Converter;

/**
 * 通过{@link FormatterRegistry} SPI,
 * 将 {@link Converter Converters}和{@link Formatter Formatters}注册到FormattingConversionService.
 */
public interface FormatterRegistrar {

	/**
	 * 通过FormatterRegistry SPI使用FormattingConversionService注册Formatter和Converter.
	 * 
	 * @param registry 要使用的FormatterRegistry实例.
	 */
	void registerFormatters(FormatterRegistry registry);

}
