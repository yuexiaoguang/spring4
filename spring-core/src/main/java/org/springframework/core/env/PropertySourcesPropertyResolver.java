package org.springframework.core.env;

import org.springframework.core.convert.ConversionException;
import org.springframework.util.ClassUtils;

/**
 * {@link PropertyResolver}实现, 它针对底层{@link PropertySources}集合解析属性值.
 */
public class PropertySourcesPropertyResolver extends AbstractPropertyResolver {

	private final PropertySources propertySources;


	/**
	 * 针对给定的属性源创建新的解析器.
	 * 
	 * @param propertySources 要使用的{@link PropertySource}对象集
	 */
	public PropertySourcesPropertyResolver(PropertySources propertySources) {
		this.propertySources = propertySources;
	}


	@Override
	public boolean containsProperty(String key) {
		if (this.propertySources != null) {
			for (PropertySource<?> propertySource : this.propertySources) {
				if (propertySource.containsProperty(key)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public String getProperty(String key) {
		return getProperty(key, String.class, true);
	}

	@Override
	public <T> T getProperty(String key, Class<T> targetValueType) {
		return getProperty(key, targetValueType, true);
	}

	@Override
	protected String getPropertyAsRawString(String key) {
		return getProperty(key, String.class, false);
	}

	protected <T> T getProperty(String key, Class<T> targetValueType, boolean resolveNestedPlaceholders) {
		if (this.propertySources != null) {
			for (PropertySource<?> propertySource : this.propertySources) {
				if (logger.isTraceEnabled()) {
					logger.trace("Searching for key '" + key + "' in PropertySource '" +
							propertySource.getName() + "'");
				}
				Object value = propertySource.getProperty(key);
				if (value != null) {
					if (resolveNestedPlaceholders && value instanceof String) {
						value = resolveNestedPlaceholders((String) value);
					}
					logKeyFound(key, propertySource, value);
					return convertValueIfNecessary(value, targetValueType);
				}
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Could not find key '" + key + "' in any property source");
		}
		return null;
	}

	@Override
	@Deprecated
	public <T> Class<T> getPropertyAsClass(String key, Class<T> targetValueType) {
		if (this.propertySources != null) {
			for (PropertySource<?> propertySource : this.propertySources) {
				if (logger.isTraceEnabled()) {
					logger.trace(String.format("Searching for key '%s' in [%s]", key, propertySource.getName()));
				}
				Object value = propertySource.getProperty(key);
				if (value != null) {
					logKeyFound(key, propertySource, value);
					Class<?> clazz;
					if (value instanceof String) {
						try {
							clazz = ClassUtils.forName((String) value, null);
						}
						catch (Exception ex) {
							throw new ClassConversionException((String) value, targetValueType, ex);
						}
					}
					else if (value instanceof Class) {
						clazz = (Class<?>) value;
					}
					else {
						clazz = value.getClass();
					}
					if (!targetValueType.isAssignableFrom(clazz)) {
						throw new ClassConversionException(clazz, targetValueType);
					}
					@SuppressWarnings("unchecked")
					Class<T> targetClass = (Class<T>) clazz;
					return targetClass;
				}
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Could not find key '%s' in any property source", key));
		}
		return null;
	}

	/**
	 * 记录在给定{@link PropertySource}中找到的给定Key, 得到给定值.
	 * <p>默认实现使用Key和源写入调试日志消息.
	 * 从4.3.3开始, 为了避免意外记录敏感设置, 这不再记录该值.
	 * 子类可以重写此方法以更改日志级别和/或日志消息, 包括属性的值.
	 * 
	 * @param key 找到的Key
	 * @param propertySource 找到Key的{@code PropertySource}
	 * @param value 对应的值
	 */
	protected void logKeyFound(String key, PropertySource<?> propertySource, Object value) {
		if (logger.isDebugEnabled()) {
			logger.debug("Found key '" + key + "' in PropertySource '" + propertySource.getName() +
					"' with value of type " + value.getClass().getSimpleName());
		}
	}


	@SuppressWarnings("serial")
	@Deprecated
	private static class ClassConversionException extends ConversionException {

		public ClassConversionException(Class<?> actual, Class<?> expected) {
			super(String.format("Actual type %s is not assignable to expected type %s",
					actual.getName(), expected.getName()));
		}

		public ClassConversionException(String actual, Class<?> expected, Exception ex) {
			super(String.format("Could not find/load class %s during attempt to convert to %s",
					actual, expected.getName()), ex);
		}
	}
}
