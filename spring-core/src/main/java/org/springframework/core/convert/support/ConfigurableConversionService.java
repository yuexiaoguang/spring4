package org.springframework.core.convert.support;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.ConverterRegistry;

/**
 * 由大多数而不是全部{@link ConversionService}类型实现的配置接口.
 * 合并{@link ConversionService}公开的只读操作和{@link ConverterRegistry}的变异操作,
 * 以便于{@link org.springframework.core.convert.converter.Converter转换器}的临时添加和删除.
 * 在应用程序上下文引导代码中处理
 * {@link org.springframework.core.env.ConfigurableEnvironment ConfigurableEnvironment}实例时, 后者特别有用.
 */
public interface ConfigurableConversionService extends ConversionService, ConverterRegistry {

}
