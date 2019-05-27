package org.springframework.format.support;

import java.util.Set;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.core.convert.support.ConversionServiceFactory;
import org.springframework.format.AnnotationFormatterFactory;
import org.springframework.format.Formatter;
import org.springframework.format.FormatterRegistrar;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.Parser;
import org.springframework.format.Printer;
import org.springframework.util.StringValueResolver;

/**
 * 提供对{@code FormattingConversionService}的便捷访问的工厂,
 * 配置了转换器和格式化器, 用于常见类型, 如数字和日期时间.
 *
 * <p>可以通过{@link #setConverters(Set)} 和 {@link #setFormatters(Set)}以声明方式注册其他转换器和格式化器.
 * 另一种选择是通过实现 {@link FormatterRegistrar}接口在代码中注册转换器和格式化器.
 * 然后可以通过 {@link #setFormatterRegistrars(Set)}配置提供一组注册商使用.
 *
 * <p>在代码中注册转换器和格式化器的一个很好的例子是 {@code JodaTimeFormatterRegistrar}, 它注册了许多与日期相关的格式化器和转换器.
 * 有关案例的更详细列表, 请参阅 {@link #setFormatterRegistrars(Set)}
 *
 * <p>与所有 {@code FactoryBean}实现一样, 此类适用于使用Spring {@code <beans>} XML配置Spring应用程序上下文时使用.
 * 使用{@link org.springframework.context.annotation.Configuration @Configuration}类配置容器时,
 * 简单地从{@link org.springframework.context.annotation.Bean @Bean}方法实例化,
 * 配置和返回相应的{@code FormattingConversionService}对象.
 */
public class FormattingConversionServiceFactoryBean
		implements FactoryBean<FormattingConversionService>, EmbeddedValueResolverAware, InitializingBean {

	private Set<?> converters;

	private Set<?> formatters;

	private Set<FormatterRegistrar> formatterRegistrars;

	private boolean registerDefaultFormatters = true;

	private StringValueResolver embeddedValueResolver;

	private FormattingConversionService conversionService;


	/**
	 * 配置应添加的自定义转换器对象.
	 * 
	 * @param converters 以下类型的实例:
	 * {@link org.springframework.core.convert.converter.Converter},
	 * {@link org.springframework.core.convert.converter.ConverterFactory},
	 * {@link org.springframework.core.convert.converter.GenericConverter}
	 */
	public void setConverters(Set<?> converters) {
		this.converters = converters;
	}

	/**
	 * 配置应添加的自定义格式化器对象.
	 * 
	 * @param formatters {@link Formatter}或{@link AnnotationFormatterFactory}的实例
	 */
	public void setFormatters(Set<?> formatters) {
		this.formatters = formatters;
	}

	/**
	 * <p>除了通过{@link #setConverters(Set)}和{@link #setFormatters(Set)}以声明方式添加的那些之外,
	 * 配置FormatterRegistrar以调用, 从而注册转换器和格式化器.
	 * <p>在为格式化类别注册多个相关转换器和格式化器时, 例如日期格式, FormatterRegistrars非常有用.
	 * 支持格式化类别所需的所有相关类型都可以从一个地方注册.
	 * <p>FormatterRegistrars 还可用于注册在与其自己的&lt;T&gt;不同的特定字段类型下索引的 Formatter,
	 * 或者从Printer/Parser对注册Formatter时.
	 */
	public void setFormatterRegistrars(Set<FormatterRegistrar> formatterRegistrars) {
		this.formatterRegistrars = formatterRegistrars;
	}

	/**
	 * 指示是否应注册默认格式化器.
	 * <p>默认情况下, 会注册内置格式化器. 此标志可用于关闭这个, 并仅依赖于显式注册的格式化器.
	 */
	public void setRegisterDefaultFormatters(boolean registerDefaultFormatters) {
		this.registerDefaultFormatters = registerDefaultFormatters;
	}

	@Override
	public void setEmbeddedValueResolver(StringValueResolver embeddedValueResolver) {
		this.embeddedValueResolver = embeddedValueResolver;
	}


	@Override
	public void afterPropertiesSet() {
		this.conversionService = new DefaultFormattingConversionService(this.embeddedValueResolver, this.registerDefaultFormatters);
		ConversionServiceFactory.registerConverters(this.converters, this.conversionService);
		registerFormatters();
	}

	private void registerFormatters() {
		if (this.formatters != null) {
			for (Object formatter : this.formatters) {
				if (formatter instanceof Formatter<?>) {
					this.conversionService.addFormatter((Formatter<?>) formatter);
				}
				else if (formatter instanceof AnnotationFormatterFactory<?>) {
					this.conversionService.addFormatterForFieldAnnotation((AnnotationFormatterFactory<?>) formatter);
				}
				else {
					throw new IllegalArgumentException(
							"Custom formatters must be implementations of Formatter or AnnotationFormatterFactory");
				}
			}
		}
		if (this.formatterRegistrars != null) {
			for (FormatterRegistrar registrar : this.formatterRegistrars) {
				registrar.registerFormatters(this.conversionService);
			}
		}
	}


	@Override
	public FormattingConversionService getObject() {
		return this.conversionService;
	}

	@Override
	public Class<? extends FormattingConversionService> getObjectType() {
		return FormattingConversionService.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
