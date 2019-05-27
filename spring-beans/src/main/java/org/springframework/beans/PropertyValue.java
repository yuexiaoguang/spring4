package org.springframework.beans;

import java.io.Serializable;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * 用于保存单个bean属性的信息和值的对象.
 * 在这里使用一个对象, 而不是仅仅将所有属性存储在由属性名称作为Key的Map中, 允许更多的灵活性, 以及以优化的方式处理索引属性等的能力.
 *
 * <p>请注意, 该值不必是最终所需的类型:
 * {@link BeanWrapper}实现应该处理任何必要的转换, 因为这个对象对它将被应用的对象一无所知.
 */
@SuppressWarnings("serial")
public class PropertyValue extends BeanMetadataAttributeAccessor implements Serializable {

	private final String name;

	private final Object value;

	private boolean optional = false;

	private boolean converted = false;

	private Object convertedValue;

	/** 包可见字段，指示是否需要转换 */
	volatile Boolean conversionNecessary;

	/** 包可见字段，用于缓存已解析的属性路径标记 */
	transient volatile Object resolvedTokens;


	/**
	 * @param name 属性名 (never {@code null})
	 * @param value 属性值 (可能在类型转换之前)
	 */
	public PropertyValue(String name, Object value) {
		this.name = name;
		this.value = value;
	}

	/**
	 * 复制.
	 * 
	 * @param original 要复制的PropertyValue (never {@code null})
	 */
	public PropertyValue(PropertyValue original) {
		Assert.notNull(original, "Original must not be null");
		this.name = original.getName();
		this.value = original.getValue();
		this.optional = original.isOptional();
		this.converted = original.converted;
		this.convertedValue = original.convertedValue;
		this.conversionNecessary = original.conversionNecessary;
		this.resolvedTokens = original.resolvedTokens;
		setSource(original.getSource());
		copyAttributesFrom(original);
	}

	/**
	 * 为原始值持有者公开新值的构造方法. 原始持有者将作为新持有者的来源.
	 * 
	 * @param original 要链接到的PropertyValue (never {@code null})
	 * @param newValue 要应用的新值
	 */
	public PropertyValue(PropertyValue original, Object newValue) {
		Assert.notNull(original, "Original must not be null");
		this.name = original.getName();
		this.value = newValue;
		this.optional = original.isOptional();
		this.conversionNecessary = original.conversionNecessary;
		this.resolvedTokens = original.resolvedTokens;
		setSource(original);
		copyAttributesFrom(original);
	}


	/**
	 * 属性名.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * 返回属性的值.
	 * <p>请注意, 此处不会发生类型转换. BeanWrapper实现负责执行类型转换.
	 */
	public Object getValue() {
		return this.value;
	}

	/**
	 * 返回此值持有者的原始PropertyValue实例.
	 * 
	 * @return 原始的PropertyValue (要么是该值持有者的来源, 要么是该值持有者本身).
	 */
	public PropertyValue getOriginalPropertyValue() {
		PropertyValue original = this;
		Object source = getSource();
		while (source instanceof PropertyValue && source != original) {
			original = (PropertyValue) source;
			source = original.getSource();
		}
		return original;
	}

	/**
	 * 设置这是否是可选值, 即, 当目标类上不存在相应的属性时被忽略.
	 * @since 3.0
	 */
	public void setOptional(boolean optional) {
		this.optional = optional;
	}

	/**
	 * 返回这是否是可选值, 即, 当目标类上不存在相应的属性时被忽略.
	 * @since 3.0
	 */
	public boolean isOptional() {
		return this.optional;
	}

	/**
	 * 返回此持有者是否已包含已转换的值 ({@code true}), 或者是否仍然需要转换值 ({@code false}).
	 */
	public synchronized boolean isConverted() {
		return this.converted;
	}

	/**
	 * 设置构造函数参数的转换值, 类型转换处理后.
	 */
	public synchronized void setConvertedValue(Object value) {
		this.converted = true;
		this.convertedValue = value;
	}

	/**
	 * 返回构造函数参数的转换值, 类型转换处理后.
	 */
	public synchronized Object getConvertedValue() {
		return this.convertedValue;
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof PropertyValue)) {
			return false;
		}
		PropertyValue otherPv = (PropertyValue) other;
		return (this.name.equals(otherPv.name) &&
				ObjectUtils.nullSafeEquals(this.value, otherPv.value) &&
				ObjectUtils.nullSafeEquals(getSource(), otherPv.getSource()));
	}

	@Override
	public int hashCode() {
		return this.name.hashCode() * 29 + ObjectUtils.nullSafeHashCode(this.value);
	}

	@Override
	public String toString() {
		return "bean property '" + this.name + "'";
	}

}
