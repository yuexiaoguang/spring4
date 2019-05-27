package org.springframework.context.event;

import java.lang.reflect.Method;

import org.springframework.context.ApplicationListener;

/**
 * 为使用{@link EventListener}注解的方法创建{@link ApplicationListener}的策略接口.
 */
public interface EventListenerFactory {

	/**
	 * 指定此工厂是否支持指定的{@link Method}.
	 * 
	 * @param method 使用{@link EventListener}注解的方法
	 * 
	 * @return {@code true} 如果此工厂支持指定的方法
	 */
	boolean supportsMethod(Method method);

	/**
	 * 为指定的方法创建{@link ApplicationListener}.
	 * 
	 * @param beanName bean的名称
	 * @param type 实例的目标类型
	 * @param method 使用{@link EventListener}注解的方法
	 * 
	 * @return 应用程序监听器, 适用于调用指定的方法
	 */
	ApplicationListener<?> createApplicationListener(String beanName, Class<?> type, Method method);

}
