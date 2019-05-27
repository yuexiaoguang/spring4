package org.springframework.beans.factory.config;

import org.springframework.beans.BeansException;

/**
 * 允许自定义修改应用程序上下文的bean定义, 调整上下文的底层bean工厂的bean属性值.
 *
 * <p>应用程序上下文可以在其bean定义中自动检测BeanFactoryPostProcessor bean, 并在创建任何其他bean之前应用它们.
 *
 * <p>对于以系统管理员为目标的自定义配置文件非常有用, 这些文件覆盖在应用程序上下文中配置的bean属性.
 *
 * <p>请参阅PropertyResourceConfigurer及其针对此类配置需求的开箱即用解决方案的具体实现.
 *
 * <p>BeanFactoryPostProcessor可以与bean定义交互并修改bean定义, 但绝不能与bean实例交互.
 * 这样做可能会导致bean过早实例化, 违反容器并造成意外的副作用.
 * 如果需要与bean实例交互, 可以实现{@link BeanPostProcessor}.
 */
public interface BeanFactoryPostProcessor {

	/**
	 * 在标准初始化之后修改应用程序上下文的内部bean工厂.
	 * 将加载所有bean定义, 但是还没有bean被实例化.
	 * 这允许覆盖或添加属性, 甚至是实时初始化bean.
	 * 
	 * @param beanFactory 应用程序上下文使用的bean工厂
	 * 
	 * @throws org.springframework.beans.BeansException 发生错误
	 */
	void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException;

}
