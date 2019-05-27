package org.springframework.beans;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;

/**
 * 用于为Spring bean创建{@link BeanInfo}实例的策略接口.
 * 可用于插入自定义bean属性解析策略 (e.g. 对于JVM上的其他语言) 或者更有效的{@link BeanInfo}检索算法.
 *
 * <p>BeanInfoFactories由{@link CachedIntrospectionResults}实例化, 
 * 通过使用{@link org.springframework.core.io.support.SpringFactoriesLoader}实用程序类.
 *
 * 当要创建{@link BeanInfo}时, {@code CachedIntrospectionResults}将遍历发现的工厂, 调用每个工厂中的{@link #getBeanInfo(Class)}.
 * 如果返回{@code null}, 下一个工厂将被查询. 如果没有工厂支持这个类, 默认创建一个标准的{@link BeanInfo}.
 *
 * <p>请注意，{@ link org.springframework.core.io.support.SpringFactoriesLoader}
 * 按{@link org.springframework.core.annotation.Order @Order}对{@code BeanInfoFactory}实例进行排序, 所以优先级较高的首先出现.
 */
public interface BeanInfoFactory {

	/**
	 * 返回给定类的bean信息.
	 * 
	 * @param beanClass the bean class
	 * 
	 * @return the BeanInfo, or {@code null}
	 * @throws IntrospectionException in case of exceptions
	 */
	BeanInfo getBeanInfo(Class<?> beanClass) throws IntrospectionException;

}
