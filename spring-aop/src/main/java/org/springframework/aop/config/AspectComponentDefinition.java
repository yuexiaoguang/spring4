package org.springframework.aop.config;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanReference;
import org.springframework.beans.factory.parsing.CompositeComponentDefinition;

/**
 * {@link org.springframework.beans.factory.parsing.ComponentDefinition}
 * 保存一个切面定义, 包括它的嵌套的切点.
 */
public class AspectComponentDefinition extends CompositeComponentDefinition {

	private final BeanDefinition[] beanDefinitions;

	private final BeanReference[] beanReferences;


	public AspectComponentDefinition(
			String aspectName, BeanDefinition[] beanDefinitions, BeanReference[] beanReferences, Object source) {

		super(aspectName, source);
		this.beanDefinitions = (beanDefinitions != null ? beanDefinitions : new BeanDefinition[0]);
		this.beanReferences = (beanReferences != null ? beanReferences : new BeanReference[0]);
	}


	@Override
	public BeanDefinition[] getBeanDefinitions() {
		return this.beanDefinitions;
	}

	@Override
	public BeanReference[] getBeanReferences() {
		return this.beanReferences;
	}
}
