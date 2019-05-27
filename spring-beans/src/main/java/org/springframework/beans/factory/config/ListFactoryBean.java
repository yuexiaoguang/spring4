package org.springframework.beans.factory.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.TypeConverter;
import org.springframework.core.ResolvableType;

/**
 * 共享List实例的简单工厂. 允许通过XML bean定义中的“list”元素集中设置List.
 */
public class ListFactoryBean extends AbstractFactoryBean<List<Object>> {

	private List<?> sourceList;

	@SuppressWarnings("rawtypes")
	private Class<? extends List> targetListClass;


	/**
	 * 设置源List, 通常通过XML“list”元素填充.
	 */
	public void setSourceList(List<?> sourceList) {
		this.sourceList = sourceList;
	}

	/**
	 * 设置要用于目标List的类. 在Spring应用程序上下文中定义时, 可以使用完全限定的类名填充.
	 * <p>默认是 {@code java.util.ArrayList}.
	 */
	@SuppressWarnings("rawtypes")
	public void setTargetListClass(Class<? extends List> targetListClass) {
		if (targetListClass == null) {
			throw new IllegalArgumentException("'targetListClass' must not be null");
		}
		if (!List.class.isAssignableFrom(targetListClass)) {
			throw new IllegalArgumentException("'targetListClass' must implement [java.util.List]");
		}
		this.targetListClass = targetListClass;
	}


	@Override
	@SuppressWarnings("rawtypes")
	public Class<List> getObjectType() {
		return List.class;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected List<Object> createInstance() {
		if (this.sourceList == null) {
			throw new IllegalArgumentException("'sourceList' is required");
		}
		List<Object> result = null;
		if (this.targetListClass != null) {
			result = BeanUtils.instantiateClass(this.targetListClass);
		}
		else {
			result = new ArrayList<Object>(this.sourceList.size());
		}
		Class<?> valueType = null;
		if (this.targetListClass != null) {
			valueType = ResolvableType.forClass(this.targetListClass).asCollection().resolveGeneric();
		}
		if (valueType != null) {
			TypeConverter converter = getBeanTypeConverter();
			for (Object elem : this.sourceList) {
				result.add(converter.convertIfNecessary(elem, valueType));
			}
		}
		else {
			result.addAll(this.sourceList);
		}
		return result;
	}

}
