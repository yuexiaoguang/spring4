package org.springframework.beans.factory.support;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;

/**
 * 负责创建与根bean定义相对应的实例的接口.
 *
 * <p>由于各种方法都是可能的, 因此将其纳入策略, 包括使用CGLIB动态创建子类以支持方法注入.
 */
public interface InstantiationStrategy {

	/**
	 * 在此工厂中返回具有给定名称的bean实例.
	 * 
	 * @param bd bean定义
	 * @param beanName 在此上下文中创建bean时的bean名称.
	 * 如果自动装配不属于工厂的bean, 则名称可以是{@code null}.
	 * @param owner 所属的BeanFactory
	 * 
	 * @return 这个bean定义的bean实例
	 * @throws BeansException 如果实例化尝试失败
	 */
	Object instantiate(RootBeanDefinition bd, String beanName, BeanFactory owner)
			throws BeansException;

	/**
	 * 在此工厂中返回具有给定名称的bean实例, 通过给定的构造函数创建它.
	 * 
	 * @param bd bean定义
	 * @param beanName 在此上下文中创建bean时的bean名称.
	 * 如果自动装配不属于工厂的bean, 则名称可以是{@code null}.
	 * @param owner 所属的BeanFactory
	 * @param ctor 要使用的构造函数
	 * @param args 要应用的构造函数参数
	 * 
	 * @return 这个bean定义的bean实例
	 * @throws BeansException 如果实例化尝试失败
	 */
	Object instantiate(RootBeanDefinition bd, String beanName, BeanFactory owner,
			Constructor<?> ctor, Object... args) throws BeansException;

	/**
	 * 在此工厂中返回具有给定名称的bean实例, 通过给定的工厂方法创建它.
	 * 
	 * @param bd bean定义
	 * @param beanName 在此上下文中创建bean时的bean名称.
	 * 如果自动装配不属于工厂的bean, 则名称可以是{@code null}.
	 * @param owner 所属的BeanFactory
	 * @param factoryBean 要调用工厂方法的工厂bean实例; 如果是静态工厂方法, 则为{@code null}
	 * @param factoryMethod 要使用的工厂方法
	 * @param args 要应用的工厂方法参数
	 * 
	 * @return 这个bean定义的bean实例
	 * @throws BeansException 如果实例化尝试失败
	 */
	Object instantiate(RootBeanDefinition bd, String beanName, BeanFactory owner,
			Object factoryBean, Method factoryMethod, Object... args) throws BeansException;

}
