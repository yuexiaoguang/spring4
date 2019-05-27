package org.springframework.aop.scope;

import java.io.Serializable;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.util.Assert;

/**
 * {@link ScopedObject}接口的默认实现.
 *
 * <p>将调用委托给底层
 * {@link ConfigurableBeanFactory bean factory}
 * ({@link ConfigurableBeanFactory#getBean(String)}/
 * {@link ConfigurableBeanFactory#destroyScopedBean(String)}).
 */
@SuppressWarnings("serial")
public class DefaultScopedObject implements ScopedObject, Serializable {

	private final ConfigurableBeanFactory beanFactory;

	private final String targetBeanName;


	/**
	 * @param beanFactory 保存有范围的目标对象的{@link ConfigurableBeanFactory}
	 * @param targetBeanName 目标bean的名称
	 */
	public DefaultScopedObject(ConfigurableBeanFactory beanFactory, String targetBeanName) {
		Assert.notNull(beanFactory, "BeanFactory must not be null");
		Assert.hasText(targetBeanName, "'targetBeanName' must not be empty");
		this.beanFactory = beanFactory;
		this.targetBeanName = targetBeanName;
	}


	@Override
	public Object getTargetObject() {
		return this.beanFactory.getBean(this.targetBeanName);
	}

	@Override
	public void removeFromScope() {
		this.beanFactory.destroyScopedBean(this.targetBeanName);
	}

}
