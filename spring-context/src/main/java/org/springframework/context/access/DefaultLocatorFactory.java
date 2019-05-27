package org.springframework.context.access;

import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.access.BeanFactoryLocator;

/**
 * 用于获取默认ContextSingletonBeanFactoryLocator实例的工厂类.
 */
public class DefaultLocatorFactory {

	/**
	 * 返回实现BeanFactoryLocator的实例对象.
	 * 这通常是使用默认资源选择器的特定ContextSingletonBeanFactoryLocator类的单例实例.
	 */
	public static BeanFactoryLocator getInstance() throws FatalBeanException {
		return ContextSingletonBeanFactoryLocator.getInstance();
	}

	/**
	 * 返回实现BeanFactoryLocator的实例对象.
	 * 这通常是使用指定的资源选择器的特定ContextSingletonBeanFactoryLocator类的单例实例.
	 * 
	 * @param selector 一个选择器变量, 它向工厂提供关于返回哪个实例的提示.
	 */
	public static BeanFactoryLocator getInstance(String selector) throws FatalBeanException {
		return ContextSingletonBeanFactoryLocator.getInstance(selector);
	}
}
