package org.springframework.beans.factory.config;

import org.springframework.util.Assert;

/**
 * 用于属性值对象的不可变占位符类, 当它是对工厂中另一个bean的引用时, 以便在运行时解析.
 */
public class RuntimeBeanReference implements BeanReference {

	private final String beanName;

	private final boolean toParent;

	private Object source;


	/**
	 * 为给定的bean名称创建一个新的RuntimeBeanReference, 而不将其显式标记为父工厂中bean的引用.
	 * 
	 * @param beanName 目标bean的名称
	 */
	public RuntimeBeanReference(String beanName) {
		this(beanName, false);
	}

	/**
	 * 为给定的bean名称创建一个新的RuntimeBeanReference, 而不将其显式标记为父工厂中bean的引用.
	 * 
	 * @param beanName 目标bean的名称
	 * @param toParent 是否是父工厂中bean的显式引用
	 */
	public RuntimeBeanReference(String beanName, boolean toParent) {
		Assert.hasText(beanName, "'beanName' must not be empty");
		this.beanName = beanName;
		this.toParent = toParent;
	}


	@Override
	public String getBeanName() {
		return this.beanName;
	}

	/**
	 * 返回是否是父工厂中bean的显式引用.
	 */
	public boolean isToParent() {
		return this.toParent;
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
		if (!(other instanceof RuntimeBeanReference)) {
			return false;
		}
		RuntimeBeanReference that = (RuntimeBeanReference) other;
		return (this.beanName.equals(that.beanName) && this.toParent == that.toParent);
	}

	@Override
	public int hashCode() {
		int result = this.beanName.hashCode();
		result = 29 * result + (this.toParent ? 1 : 0);
		return result;
	}

	@Override
	public String toString() {
		return '<' + getBeanName() + '>';
	}

}
