package org.springframework.beans.factory.config;

import org.springframework.beans.BeansException;

/**
 * 用于通过将其作为表达式进行评估来解析值的策略接口.
 *
 * <p>原始{@link org.springframework.beans.factory.BeanFactory}不包含此策略的默认实现.
 * 但是, {@link org.springframework.context.ApplicationContext} 实现将提供开箱即用的表达式支持.
 */
public interface BeanExpressionResolver {

	/**
	 * 如果适用, 将给定值评估为表达式; 否则返回给定的值.
	 * 
	 * @param value 要检查的值
	 * @param evalContext 评估上下文
	 * 
	 * @return 解析后的值 (可能是给定的值)
	 * @throws BeansException 如果评估失败
	 */
	Object evaluate(String value, BeanExpressionContext evalContext) throws BeansException;

}
