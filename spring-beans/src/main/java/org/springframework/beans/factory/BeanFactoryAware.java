package org.springframework.beans.factory;

import org.springframework.beans.BeansException;

/**
 * 希望知道自己所属的{@link BeanFactory}的Bean需要实现的接口.
 *
 * <p>例如, bean可以通过工厂查找依赖的bean (依赖查找).
 * 请注意, 大多数bean将选择通过相应的bean属性或构造函数参数接收对协作bean的引用 (依赖注入).
 *
 * <p>有关所有Bean生命周期方法的列表, see the {@link BeanFactory BeanFactory javadocs}.
 */
public interface BeanFactoryAware extends Aware {

	/**
	 * 将所属的工厂提供给bean实例的回调.
	 * <p>在普通bean属性填充之后, 在初始化回调之前调用,
	 * 例如{@link InitializingBean#afterPropertiesSet()} 或自定义初始化方法.
	 * 
	 * @param beanFactory 所属的BeanFactory (never {@code null}). bean可以立即调用工厂的方法.
	 * 
	 * @throws BeansException 初始化错误
	 */
	void setBeanFactory(BeanFactory beanFactory) throws BeansException;

}
