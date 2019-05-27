package org.springframework.beans.factory.config;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.TypeConverter;
import org.springframework.core.ResolvableType;

/**
 * 共享Set实例的简单工厂. 允许通过XML bean定义中的“set”元素集中设置集合.
 */
public class SetFactoryBean extends AbstractFactoryBean<Set<Object>> {

	private Set<?> sourceSet;

	@SuppressWarnings("rawtypes")
	private Class<? extends Set> targetSetClass;


	/**
	 * 设置源集合, 通常通过XML “set”元素填充.
	 */
	public void setSourceSet(Set<?> sourceSet) {
		this.sourceSet = sourceSet;
	}

	/**
	 * 设置要用于目标Set的类. 在Spring应用程序上下文中定义时, 可以使用完全限定的类名填充.
	 * <p>默认是 linked HashSet, 保持注册顺序.
	 */
	@SuppressWarnings("rawtypes")
	public void setTargetSetClass(Class<? extends Set> targetSetClass) {
		if (targetSetClass == null) {
			throw new IllegalArgumentException("'targetSetClass' must not be null");
		}
		if (!Set.class.isAssignableFrom(targetSetClass)) {
			throw new IllegalArgumentException("'targetSetClass' must implement [java.util.Set]");
		}
		this.targetSetClass = targetSetClass;
	}


	@Override
	@SuppressWarnings("rawtypes")
	public Class<Set> getObjectType() {
		return Set.class;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected Set<Object> createInstance() {
		if (this.sourceSet == null) {
			throw new IllegalArgumentException("'sourceSet' is required");
		}
		Set<Object> result = null;
		if (this.targetSetClass != null) {
			result = BeanUtils.instantiateClass(this.targetSetClass);
		}
		else {
			result = new LinkedHashSet<Object>(this.sourceSet.size());
		}
		Class<?> valueType = null;
		if (this.targetSetClass != null) {
			valueType = ResolvableType.forClass(this.targetSetClass).asCollection().resolveGeneric();
		}
		if (valueType != null) {
			TypeConverter converter = getBeanTypeConverter();
			for (Object elem : this.sourceSet) {
				result.add(converter.convertIfNecessary(elem, valueType));
			}
		}
		else {
			result.addAll(this.sourceSet);
		}
		return result;
	}

}
