package org.springframework.core.env;

import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.util.SystemPropertyUtils;

/**
 * 用于解析任何底层源的属性的抽象基类.
 */
public abstract class AbstractPropertyResolver implements ConfigurablePropertyResolver {

	protected final Log logger = LogFactory.getLog(getClass());

	private volatile ConfigurableConversionService conversionService;

	private PropertyPlaceholderHelper nonStrictHelper;

	private PropertyPlaceholderHelper strictHelper;

	private boolean ignoreUnresolvableNestedPlaceholders = false;

	private String placeholderPrefix = SystemPropertyUtils.PLACEHOLDER_PREFIX;

	private String placeholderSuffix = SystemPropertyUtils.PLACEHOLDER_SUFFIX;

	private String valueSeparator = SystemPropertyUtils.VALUE_SEPARATOR;

	private final Set<String> requiredProperties = new LinkedHashSet<String>();


	@Override
	public ConfigurableConversionService getConversionService() {
		// 需要提供独立的DefaultConversionService, 而不是PropertySourcesPropertyResolver使用的共享DefaultConversionService.
		if (this.conversionService == null) {
			synchronized (this) {
				if (this.conversionService == null) {
					this.conversionService = new DefaultConversionService();
				}
			}
		}
		return conversionService;
	}

	@Override
	public void setConversionService(ConfigurableConversionService conversionService) {
		Assert.notNull(conversionService, "ConversionService must not be null");
		this.conversionService = conversionService;
	}

	/**
	 * 设置此解析器替换的占位符必须以此开头的前缀.
	 * <p>默认是 "${".
	 */
	@Override
	public void setPlaceholderPrefix(String placeholderPrefix) {
		Assert.notNull(placeholderPrefix, "'placeholderPrefix' must not be null");
		this.placeholderPrefix = placeholderPrefix;
	}

	/**
	 * 设置由此解析器替换的占位符必须以此结尾的后缀.
	 * <p>默认是 "}".
	 */
	@Override
	public void setPlaceholderSuffix(String placeholderSuffix) {
		Assert.notNull(placeholderSuffix, "'placeholderSuffix' must not be null");
		this.placeholderSuffix = placeholderSuffix;
	}

	/**
	 * 指定由此解析器替换的占位符与其关联的默认值之间的分隔字符;
	 * 如果不应将此类特殊字符作为值分隔符处理, 则指定{@code null}.
	 * <p>默认是 ":".
	 */
	@Override
	public void setValueSeparator(String valueSeparator) {
		this.valueSeparator = valueSeparator;
	}

	/**
	 * 设置当遇到嵌套在给定属性值内的不可解析的占位符时是否抛出异常.
	 * {@code false}值表示严格的解析, i.e. 将抛出异常.
	 * {@code true}值表示不可解析的嵌套占位符应以未解析的${...}形式传递.
	 * <p>默认是{@code false}.
	 */
	@Override
	public void setIgnoreUnresolvableNestedPlaceholders(boolean ignoreUnresolvableNestedPlaceholders) {
		this.ignoreUnresolvableNestedPlaceholders = ignoreUnresolvableNestedPlaceholders;
	}

	@Override
	public void setRequiredProperties(String... requiredProperties) {
		if (requiredProperties != null) {
			for (String key : requiredProperties) {
				this.requiredProperties.add(key);
			}
		}
	}

	@Override
	public void validateRequiredProperties() {
		MissingRequiredPropertiesException ex = new MissingRequiredPropertiesException();
		for (String key : this.requiredProperties) {
			if (this.getProperty(key) == null) {
				ex.addMissingRequiredProperty(key);
			}
		}
		if (!ex.getMissingRequiredProperties().isEmpty()) {
			throw ex;
		}
	}

	@Override
	public boolean containsProperty(String key) {
		return (getProperty(key) != null);
	}

	@Override
	public String getProperty(String key) {
		return getProperty(key, String.class);
	}

	@Override
	public String getProperty(String key, String defaultValue) {
		String value = getProperty(key);
		return (value != null ? value : defaultValue);
	}

	@Override
	public <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
		T value = getProperty(key, targetType);
		return (value != null ? value : defaultValue);
	}

	@Override
	@Deprecated
	public <T> Class<T> getPropertyAsClass(String key, Class<T> targetValueType) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getRequiredProperty(String key) throws IllegalStateException {
		String value = getProperty(key);
		if (value == null) {
			throw new IllegalStateException("Required key '" + key + "' not found");
		}
		return value;
	}

	@Override
	public <T> T getRequiredProperty(String key, Class<T> valueType) throws IllegalStateException {
		T value = getProperty(key, valueType);
		if (value == null) {
			throw new IllegalStateException("Required key '" + key + "' not found");
		}
		return value;
	}

	@Override
	public String resolvePlaceholders(String text) {
		if (this.nonStrictHelper == null) {
			this.nonStrictHelper = createPlaceholderHelper(true);
		}
		return doResolvePlaceholders(text, this.nonStrictHelper);
	}

	@Override
	public String resolveRequiredPlaceholders(String text) throws IllegalArgumentException {
		if (this.strictHelper == null) {
			this.strictHelper = createPlaceholderHelper(false);
		}
		return doResolvePlaceholders(text, this.strictHelper);
	}

	/**
	 * 解析给定字符串中的占位符, 推迟到{@link #setIgnoreUnresolvableNestedPlaceholders}的值,
	 * 以确定是否有任何不可解析的占位符引发异常或被忽略.
	 * <p>从{@link #getProperty}及其变体调用, 隐式解析嵌套占位符.
	 * 相反, {@link #resolvePlaceholders}和{@link #resolveRequiredPlaceholders} <emphasis>不</emphasis>委托给此方法,
	 * 而是执行自己对不可解析的占位符的处理, 这是由每个方法指定的.
	 */
	protected String resolveNestedPlaceholders(String value) {
		return (this.ignoreUnresolvableNestedPlaceholders ?
				resolvePlaceholders(value) : resolveRequiredPlaceholders(value));
	}

	private PropertyPlaceholderHelper createPlaceholderHelper(boolean ignoreUnresolvablePlaceholders) {
		return new PropertyPlaceholderHelper(this.placeholderPrefix, this.placeholderSuffix,
				this.valueSeparator, ignoreUnresolvablePlaceholders);
	}

	private String doResolvePlaceholders(String text, PropertyPlaceholderHelper helper) {
		return helper.replacePlaceholders(text, new PropertyPlaceholderHelper.PlaceholderResolver() {
			@Override
			public String resolvePlaceholder(String placeholderName) {
				return getPropertyAsRawString(placeholderName);
			}
		});
	}

	/**
	 * 将给定值转换为指定的目标类型.
	 * 
	 * @param value 原始的属性值
	 * @param targetType 属性检索的指定目标类型
	 * 
	 * @return 转换后的值, 或原始值
	 */
	@SuppressWarnings("unchecked")
	protected <T> T convertValueIfNecessary(Object value, Class<T> targetType) {
		if (targetType == null) {
			return (T) value;
		}
		ConversionService conversionServiceToUse = this.conversionService;
		if (conversionServiceToUse == null) {
			// 如果首先不需要标准类型转换, 请避免初始化共享DefaultConversionService...
			if (ClassUtils.isAssignableValue(targetType, value)) {
				return (T) value;
			}
			conversionServiceToUse = DefaultConversionService.getSharedInstance();
		}
		return conversionServiceToUse.convert(value, targetType);
	}


	/**
	 * 将指定的属性检索为原始String, i.e. 不解析嵌套占位符.
	 * 
	 * @param key 要解析的属性名称
	 * 
	 * @return 属性值或{@code null}
	 */
	protected abstract String getPropertyAsRawString(String key);

}
