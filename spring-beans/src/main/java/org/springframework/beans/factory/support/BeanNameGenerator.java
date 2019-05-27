package org.springframework.beans.factory.support;

import org.springframework.beans.factory.config.BeanDefinition;

/**
 * 用于为bean定义生成bean名称的策略接口.
 */
public interface BeanNameGenerator {

	/**
	 * 为给定的bean定义生成bean名称.
	 * 
	 * @param definition 用于生成名称的bean定义
	 * @param registry 给定的定义要注册的bean定义注册表
	 * 
	 * @return 生成的bean名称
	 */
	String generateBeanName(BeanDefinition definition, BeanDefinitionRegistry registry);

}
