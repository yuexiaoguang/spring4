package org.springframework.aop.target.dynamic;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.util.Assert;

/**
 * 可刷新的TargetSource, 用于从BeanFactory获取新的目标bean.
 *
 * <p>可以进行子类化, 以覆盖{@code requiresRefresh()}, 以抑制不必要的刷新.
 * 默认情况下, 每次“refreshCheckDelay”结束时都会执行刷新.
 */
public class BeanFactoryRefreshableTargetSource extends AbstractRefreshableTargetSource {

	private final BeanFactory beanFactory;

	private final String beanName;


	/**
	 * <p>请注意，传入的BeanFactory应该为给定的bean名称设置适当的bean定义.
	 * 
	 * @param beanFactory 从中获取bean的BeanFactory
	 * @param beanName 目标bean的名称
	 */
	public BeanFactoryRefreshableTargetSource(BeanFactory beanFactory, String beanName) {
		Assert.notNull(beanFactory, "BeanFactory is required");
		Assert.notNull(beanName, "Bean name is required");
		this.beanFactory = beanFactory;
		this.beanName = beanName;
	}


	/**
	 * 检索新的目标对象.
	 */
	@Override
	protected final Object freshTarget() {
		return this.obtainFreshBean(this.beanFactory, this.beanName);
	}

	/**
	 * 子类可以重写的模板方法，为给定的bean工厂和bean名称提供新的目标对象.
	 * <p>此默认实现从bean工厂中获取新的目标bean实例.
	 */
	protected Object obtainFreshBean(BeanFactory beanFactory, String beanName) {
		return beanFactory.getBean(beanName);
	}

}
