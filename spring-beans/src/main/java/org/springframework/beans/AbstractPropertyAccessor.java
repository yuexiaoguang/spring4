package org.springframework.beans;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * {@link PropertyAccessor}接口的抽象实现.
 * 提供所有便捷方法的基本实现, 实际属性访问的实现留给子类.
 */
public abstract class AbstractPropertyAccessor extends TypeConverterSupport implements ConfigurablePropertyAccessor {

	private boolean extractOldValueForEditor = false;

	private boolean autoGrowNestedPaths = false;


	@Override
	public void setExtractOldValueForEditor(boolean extractOldValueForEditor) {
		this.extractOldValueForEditor = extractOldValueForEditor;
	}

	@Override
	public boolean isExtractOldValueForEditor() {
		return this.extractOldValueForEditor;
	}

	@Override
	public void setAutoGrowNestedPaths(boolean autoGrowNestedPaths) {
		this.autoGrowNestedPaths = autoGrowNestedPaths;
	}

	@Override
	public boolean isAutoGrowNestedPaths() {
		return this.autoGrowNestedPaths;
	}


	@Override
	public void setPropertyValue(PropertyValue pv) throws BeansException {
		setPropertyValue(pv.getName(), pv.getValue());
	}

	@Override
	public void setPropertyValues(Map<?, ?> map) throws BeansException {
		setPropertyValues(new MutablePropertyValues(map));
	}

	@Override
	public void setPropertyValues(PropertyValues pvs) throws BeansException {
		setPropertyValues(pvs, false, false);
	}

	@Override
	public void setPropertyValues(PropertyValues pvs, boolean ignoreUnknown) throws BeansException {
		setPropertyValues(pvs, ignoreUnknown, false);
	}

	@Override
	public void setPropertyValues(PropertyValues pvs, boolean ignoreUnknown, boolean ignoreInvalid)
			throws BeansException {

		List<PropertyAccessException> propertyAccessExceptions = null;
		List<PropertyValue> propertyValues = (pvs instanceof MutablePropertyValues ?
				((MutablePropertyValues) pvs).getPropertyValueList() : Arrays.asList(pvs.getPropertyValues()));
		for (PropertyValue pv : propertyValues) {
			try {
				// 此方法可能会抛出任何BeansException, 在这里不会捕获, 如果存在严重故障, 例如没有匹配的字段.
				// 可以尝试只处理不那么严重的异常情况.
				setPropertyValue(pv);
			}
			catch (NotWritablePropertyException ex) {
				if (!ignoreUnknown) {
					throw ex;
				}
				// Otherwise, just ignore it and continue...
			}
			catch (NullValueInNestedPathException ex) {
				if (!ignoreInvalid) {
					throw ex;
				}
				// Otherwise, just ignore it and continue...
			}
			catch (PropertyAccessException ex) {
				if (propertyAccessExceptions == null) {
					propertyAccessExceptions = new LinkedList<PropertyAccessException>();
				}
				propertyAccessExceptions.add(ex);
			}
		}

		// 如果遇到个别异常, 抛出复合异常.
		if (propertyAccessExceptions != null) {
			PropertyAccessException[] paeArray =
					propertyAccessExceptions.toArray(new PropertyAccessException[propertyAccessExceptions.size()]);
			throw new PropertyBatchUpdateException(paeArray);
		}
	}


	// Redefined with public visibility.
	@Override
	public Class<?> getPropertyType(String propertyPath) {
		return null;
	}

	/**
	 * 实际获取属性的值.
	 * 
	 * @param propertyName 属性名
	 * 
	 * @return 属性的值
	 * @throws InvalidPropertyException 如果没有这样的属性或属性不可读
	 * @throws PropertyAccessException 如果属性有效但访问方法失败
	 */
	@Override
	public abstract Object getPropertyValue(String propertyName) throws BeansException;

	/**
	 * 实际设置属性的值.
	 * 
	 * @param propertyName 属性名
	 * @param value 属性的值
	 * 
	 * @throws InvalidPropertyException 如果没有这样的属性或属性不可写
	 * @throws PropertyAccessException 如果属性有效但访问方法失败或类型不匹配
	 */
	@Override
	public abstract void setPropertyValue(String propertyName, Object value) throws BeansException;

}
