package org.springframework.context.support;

import java.util.Set;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.ConversionServiceFactory;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;

/**
 * 提供对配置了适用于大多数环境的转换器的ConversionService的便捷访问的工厂.
 * 设置{@link #setConverters "converters"}属性以补充默认转换器.
 *
 * <p>此实现创建 {@link DefaultConversionService}.
 * 子类可以重写 {@link #createConversionService()} 以返回他们选择的 {@link GenericConversionService}实例.
 *
 * <p>与所有 {@code FactoryBean}实现一样, 此类适用于使用Spring {@code <beans>} XML配置Spring应用程序上下文时使用.
 * 使用{@link org.springframework.context.annotation.Configuration @Configuration}类配置容器时,
 * 简单地从{@link org.springframework.context.annotation.Bean @Bean}方法实例化, 配置和返回相应的{@code ConversionService}对象.
 */
public class ConversionServiceFactoryBean implements FactoryBean<ConversionService>, InitializingBean {

	private Set<?> converters;

	private GenericConversionService conversionService;


	/**
	 * 配置应添加的自定义转换器对象集合:
	 * 实现{@link org.springframework.core.convert.converter.Converter},
	 * {@link org.springframework.core.convert.converter.ConverterFactory},
	 * 或{@link org.springframework.core.convert.converter.GenericConverter}.
	 */
	public void setConverters(Set<?> converters) {
		this.converters = converters;
	}

	@Override
	public void afterPropertiesSet() {
		this.conversionService = createConversionService();
		ConversionServiceFactory.registerConverters(this.converters, this.conversionService);
	}

	/**
	 * 创建此工厂bean返回的ConversionService实例.
	 * <p>默认情况下创建一个简单的{@link GenericConversionService}实例.
	 * 子类可以重写以自定义创建的ConversionService实例.
	 */
	protected GenericConversionService createConversionService() {
		return new DefaultConversionService();
	}


	// implementing FactoryBean

	@Override
	public ConversionService getObject() {
		return this.conversionService;
	}

	@Override
	public Class<? extends ConversionService> getObjectType() {
		return GenericConversionService.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
