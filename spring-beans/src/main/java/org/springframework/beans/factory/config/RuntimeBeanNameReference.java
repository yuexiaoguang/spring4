package org.springframework.beans.factory.config;

import org.springframework.util.Assert;

/**
 * 用于属性值对象的不可变占位符类, 当它是对工厂中另一个bean名称的引用时, 将在运行时解析.
 */
public class RuntimeBeanNameReference implements BeanReference {

	private final String beanName;

	private Object source;


	/**
	 * @param beanName 目标bean的名称
	 */
	public RuntimeBeanNameReference(String beanName) {
		Assert.hasText(beanName, "'beanName' must not be empty");
		this.beanName = beanName;
	}

	@Override
	public String getBeanName() {
		return this.beanName;
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
		if (!(other instanceof RuntimeBeanNameReference)) {
			return false;
		}
		RuntimeBeanNameReference that = (RuntimeBeanNameReference) other;
		return this.beanName.equals(that.beanName);
	}

	@Override
	public int hashCode() {
		return this.beanName.hashCode();
	}

	@Override
	public String toString() {
		return '<' + getBeanName() + '>';
	}

}
