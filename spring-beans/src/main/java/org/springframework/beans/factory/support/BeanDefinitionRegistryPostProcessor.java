package org.springframework.beans.factory.support;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;

/**
 * 扩展标准的{@link BeanFactoryPostProcessor} SPI, 允许在常规BeanFactoryPostProcessor检测开始之前注册其他bean定义.
 * 特别是, BeanDefinitionRegistryPostProcessor可以注册更多的bean定义, 而bean定义又定义了BeanFactoryPostProcessor实例.
 */
public interface BeanDefinitionRegistryPostProcessor extends BeanFactoryPostProcessor {

	/**
	 * 在标准初始化之后修改应用程序上下文的内部bean定义注册表.
	 * 将加载所有常规bean定义, 但尚未实例化任何bean.
	 * 允许在下一个后处理阶段开始之前添加更多的bean定义.
	 * 
	 * @param registry 应用程序上下文使用的bean定义注册表
	 * 
	 * @throws org.springframework.beans.BeansException 发生错误
	 */
	void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException;

}
