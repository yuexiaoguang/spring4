package org.springframework.beans.factory.support;

import org.springframework.beans.factory.config.BeanDefinition;

/**
 * GenericBeanDefinition是用于标准bean定义的一站式商店.
 * 与任何bean定义一样, 它允许指定类以及可选的构造函数参数值和属性值.
 * 此外, 可以通过“parentName”属性灵活配置从父级bean定义派生.
 *
 * <p>通常, 使用此{@code GenericBeanDefinition}类来注册用户可见的bean定义
 * (后处理器可以操作, 甚至可能重新配置父级名称).
 * 使用 {@code RootBeanDefinition} / {@code ChildBeanDefinition}, 其中父/子关系恰好是预先确定的.
 */
@SuppressWarnings("serial")
public class GenericBeanDefinition extends AbstractBeanDefinition {

	private String parentName;


	/**
	 * 通过其bean属性和配置方法进行配置.
	 */
	public GenericBeanDefinition() {
		super();
	}

	/**
	 * 深克隆给定的bean定义.
	 * 
	 * @param original 要从中复制的原始bean定义
	 */
	public GenericBeanDefinition(BeanDefinition original) {
		super(original);
	}


	@Override
	public void setParentName(String parentName) {
		this.parentName = parentName;
	}

	@Override
	public String getParentName() {
		return this.parentName;
	}


	@Override
	public AbstractBeanDefinition cloneBeanDefinition() {
		return new GenericBeanDefinition(this);
	}

	@Override
	public boolean equals(Object other) {
		return (this == other || (other instanceof GenericBeanDefinition && super.equals(other)));
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("Generic bean");
		if (this.parentName != null) {
			sb.append(" with parent '").append(this.parentName).append("'");
		}
		sb.append(": ").append(super.toString());
		return sb.toString();
	}

}
