package org.springframework.beans.factory.support;

import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * 运行时合并bean定义的后处理器回调接口.
 * {@link BeanPostProcessor}实现可以实现此子接口,
 * 以便对Spring {@code BeanFactory}用于创建bean实例的合并bean定义(原始bean定义的已处理副本)进行后处理.
 *
 * <p>{@link #postProcessMergedBeanDefinition}方法可以反射bean定义, 以便在对bean的实际实例进行后处理之前, 准备一些缓存的元数据.
 * 它也允许修改bean定义, 但仅限于实际用于并发修改的定义属性.
 * 从本质上讲, 这仅适用于{@link RootBeanDefinition}本身定义的操作, 但不适用于其基类的属性.
 */
public interface MergedBeanDefinitionPostProcessor extends BeanPostProcessor {

	/**
	 * 对指定bean的给定的合并bean定义进行后处理.
	 * 
	 * @param beanDefinition bean的合并bean定义
	 * @param beanType 托管的bean实例的实际类型
	 * @param beanName bean的名称
	 */
	void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName);

}
