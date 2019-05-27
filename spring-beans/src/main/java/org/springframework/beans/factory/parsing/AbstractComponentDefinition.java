package org.springframework.beans.factory.parsing;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanReference;

/**
 * {@link ComponentDefinition}的基本实现, 提供{@link #getDescription}的基本实现, 该实现委托给{@link #getName}.
 * 还提供{@link #toString}的基本实现, 该实现委托给{@link #getDescription}以符合建议的实现策略.
 * 还提供了返回空数组的{@link #getInnerBeanDefinitions}和{@link #getBeanReferences}的默认实现.
 */
public abstract class AbstractComponentDefinition implements ComponentDefinition {

	/**
	 * 委托给 {@link #getName}.
	 */
	@Override
	public String getDescription() {
		return getName();
	}

	/**
	 * 返回空数组.
	 */
	@Override
	public BeanDefinition[] getBeanDefinitions() {
		return new BeanDefinition[0];
	}

	/**
	 * 返回空数组.
	 */
	@Override
	public BeanDefinition[] getInnerBeanDefinitions() {
		return new BeanDefinition[0];
	}

	/**
	 * 返回空数组.
	 */
	@Override
	public BeanReference[] getBeanReferences() {
		return new BeanReference[0];
	}

	/**
	 * 委托给 {@link #getDescription}.
	 */
	@Override
	public String toString() {
		return getDescription();
	}

}
