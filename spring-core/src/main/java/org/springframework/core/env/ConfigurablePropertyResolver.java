package org.springframework.core.env;

import org.springframework.core.convert.support.ConfigurableConversionService;

/**
 * 由大多数(不是全部){@link PropertyResolver}类型实现的配置接口.
 * 将属性值从一种类型转换为另一种类型时, 提供用于访问和自定义使用的
 * {@link org.springframework.core.convert.ConversionService ConversionService}的工具.
 */
public interface ConfigurablePropertyResolver extends PropertyResolver {

	/**
	 * 返回在属性上执行类型转换时使用的{@link ConfigurableConversionService}.
	 * <p>返回的转换服务的可配置特性允许方便地添加和删除单个{@code Converter}实例:
	 * <pre class="code">
	 * ConfigurableConversionService cs = env.getConversionService();
	 * cs.addConverter(new FooConverter());
	 * </pre>
	 */
	ConfigurableConversionService getConversionService();

	/**
	 * 设置在属性上执行类型转换时要使用的{@link ConfigurableConversionService}.
	 * <p><strong>Note:</strong> 作为完全替换{@code ConversionService}的替代方法,
	 * 请考虑通过钻取{@link #getConversionService()}并调用{@code #addConverter}等方法来添加或删除单个{@code Converter}实例.
	 */
	void setConversionService(ConfigurableConversionService conversionService);

	/**
	 * 设置由此解析器替换的占位符必须以此开头的前缀.
	 */
	void setPlaceholderPrefix(String placeholderPrefix);

	/**
	 * 设置由此解析器替换的占位符必须以此结尾的后缀.
	 */
	void setPlaceholderSuffix(String placeholderSuffix);

	/**
	 * 指定由此解析程序替换的占位符与其关联的默认值之间的分隔字符;
	 * 如果不应将此类特殊字符作为值分隔符处理, 则指定{@code null}.
	 */
	void setValueSeparator(String valueSeparator);

	/**
	 * 设置当遇到嵌套在给定属性值内的不可解析的占位符时是否抛出异常.
	 * {@code false}值表示严格的解析, 即将抛出异常.
	 * {@code true}值表示不可解析的嵌套占位符应以未解析的$ {...}形式传递.
	 * <p>{@link #getProperty(String)}及其变体的实现必须检查此处设置的值, 以确定属性值包含不可解析的占位符时的正确行为.
	 */
	void setIgnoreUnresolvableNestedPlaceholders(boolean ignoreUnresolvableNestedPlaceholders);

	/**
	 * 指定必须存在哪些属性, 以便{@link #validateRequiredProperties()}进行验证.
	 */
	void setRequiredProperties(String... requiredProperties);

	/**
	 * 验证{@link #setRequiredProperties}指定的每个属性是否存在, 并解析为非{@code null}值.
	 * 
	 * @throws MissingRequiredPropertiesException 如果任何所需的属性不可解析.
	 */
	void validateRequiredProperties() throws MissingRequiredPropertiesException;

}
