package org.springframework.beans.factory.config;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.TypeConverter;
import org.springframework.core.ResolvableType;

/**
 * 共享Map实例的简单工厂. 允许通过XML bean定义中的“map”元素集中设置Map.
 */
public class MapFactoryBean extends AbstractFactoryBean<Map<Object, Object>> {

	private Map<?, ?> sourceMap;

	@SuppressWarnings("rawtypes")
	private Class<? extends Map> targetMapClass;


	/**
	 * 设置源Map, 通常通过XML“map”元素填充.
	 */
	public void setSourceMap(Map<?, ?> sourceMap) {
		this.sourceMap = sourceMap;
	}

	/**
	 * 设置要用于目标Map的类. 在Spring应用程序上下文中定义时, 可以使用完全限定的类名填充.
	 * <p>默认是 linked HashMap, 保留注册顺序.
	 */
	@SuppressWarnings("rawtypes")
	public void setTargetMapClass(Class<? extends Map> targetMapClass) {
		if (targetMapClass == null) {
			throw new IllegalArgumentException("'targetMapClass' must not be null");
		}
		if (!Map.class.isAssignableFrom(targetMapClass)) {
			throw new IllegalArgumentException("'targetMapClass' must implement [java.util.Map]");
		}
		this.targetMapClass = targetMapClass;
	}


	@Override
	@SuppressWarnings("rawtypes")
	public Class<Map> getObjectType() {
		return Map.class;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected Map<Object, Object> createInstance() {
		if (this.sourceMap == null) {
			throw new IllegalArgumentException("'sourceMap' is required");
		}
		Map<Object, Object> result = null;
		if (this.targetMapClass != null) {
			result = BeanUtils.instantiateClass(this.targetMapClass);
		}
		else {
			result = new LinkedHashMap<Object, Object>(this.sourceMap.size());
		}
		Class<?> keyType = null;
		Class<?> valueType = null;
		if (this.targetMapClass != null) {
			ResolvableType mapType = ResolvableType.forClass(this.targetMapClass).asMap();
			keyType = mapType.resolveGeneric(0);
			valueType = mapType.resolveGeneric(1);
		}
		if (keyType != null || valueType != null) {
			TypeConverter converter = getBeanTypeConverter();
			for (Map.Entry<?, ?> entry : this.sourceMap.entrySet()) {
				Object convertedKey = converter.convertIfNecessary(entry.getKey(), keyType);
				Object convertedValue = converter.convertIfNecessary(entry.getValue(), valueType);
				result.put(convertedKey, convertedValue);
			}
		}
		else {
			result.putAll(this.sourceMap);
		}
		return result;
	}

}
