package org.springframework.beans;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * 持有key-value格式属性的持有者, 该属性是bean定义的一部分.
 * 除了键值对之外, 还要跟踪定义源.
 */
public class BeanMetadataAttribute implements BeanMetadataElement {

	private final String name;

	private final Object value;

	private Object source;


	/**
	 * @param name 属性名 (never {@code null})
	 * @param value 属性值 (可能在类型转换之前)
	 */
	public BeanMetadataAttribute(String name, Object value) {
		Assert.notNull(name, "Name must not be null");
		this.name = name;
		this.value = value;
	}


	/**
	 * 返回属性的名称.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * 返回属性值.
	 */
	public Object getValue() {
		return this.value;
	}

	/**
	 * 为此元数据元素设置配置源{@code Object}.
	 * <p>对象的确切类型取决于所使用的配置机制.
	 */
	public void setSource(Object source) {
		this.source = source;
	}

	@Override
	public Object getSource() {
		return this.source;
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof BeanMetadataAttribute)) {
			return false;
		}
		BeanMetadataAttribute otherMa = (BeanMetadataAttribute) other;
		return (this.name.equals(otherMa.name) &&
				ObjectUtils.nullSafeEquals(this.value, otherMa.value) &&
				ObjectUtils.nullSafeEquals(this.source, otherMa.source));
	}

	@Override
	public int hashCode() {
		return this.name.hashCode() * 29 + ObjectUtils.nullSafeHashCode(this.value);
	}

	@Override
	public String toString() {
		return "metadata attribute '" + this.name + "'";
	}

}
