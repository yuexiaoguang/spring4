package org.springframework.beans.factory.support;

import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.DependencyDescriptor;

/**
 * 用于确定特定bean定义是否有资格作为特定依赖关系的自动装配候选的策略接口.
 */
public interface AutowireCandidateResolver {

	/**
	 * 确定给定的b​​ean定义是否有资格作为给定依赖项的autowire候选.
	 * 
	 * @param bdHolder bean定义, 包括bean名称和别名
	 * @param descriptor 目标方法参数或字段的描述符
	 * 
	 * @return bean定义是否有资格作为autowire候选者
	 */
	boolean isAutowireCandidate(BeanDefinitionHolder bdHolder, DependencyDescriptor descriptor);

	/**
	 * 确定是否为给定的依赖项建议了默认值.
	 * 
	 * @param descriptor 目标方法参数或字段的描述符
	 * 
	 * @return 建议的值 (通常是一个表达式字符串), 或{@code null}
	 * @since 3.0
	 */
	Object getSuggestedValue(DependencyDescriptor descriptor);

	/**
	 * 如果注入点需要, 为实际依赖关系目标的延迟解析构建代理.
	 * 
	 * @param descriptor 目标方法参数或字段的描述符
	 * @param beanName 包含注入点的bean的名称
	 * 
	 * @return 实际依赖项目标的延迟解析代理, 或{@code null} 如果要执行直接解析
	 * @since 4.0
	 */
	Object getLazyResolutionProxyIfNecessary(DependencyDescriptor descriptor, String beanName);

}
