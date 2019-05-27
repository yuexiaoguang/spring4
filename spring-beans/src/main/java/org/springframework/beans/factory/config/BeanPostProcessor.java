package org.springframework.beans.factory.config;

import org.springframework.beans.BeansException;

/**
 * 工厂钩子, 允许自定义修改新的bean实例, e.g. 检查标记接口或用代理包装它们.
 *
 * <p>ApplicationContexts可以在其bean定义中自动检测BeanPostProcessor bean, 并将它们应用于随后创建的任何bean.
 * 普通bean工厂允许后处理器的程序化注册, 适用于通过该工厂创建的所有bean.
 *
 * <p>通常, 通过标记接口等填充bean的后处理器将实现 {@link #postProcessBeforeInitialization},
 * 而使用代理包装bean的后处理器通常会实现 {@link #postProcessAfterInitialization}.
 */
public interface BeanPostProcessor {

	/**
	 * 在任何bean初始化回调之前, 将此BeanPostProcessor应用于给定的新bean实例
	 * (比如InitializingBean的{@code afterPropertiesSet}或自定义init方法).
	 * bean已经填充了属性值. 返回的bean实例可能是原始实例的包装器.
	 * 
	 * @param bean 新bean实例
	 * @param beanName bean的名称
	 * 
	 * @return 要使用的bean实例, 无论是原始的还是包装后的;
	 * 如果是{@code null}, 不会调用后续的BeanPostProcessors
	 * @throws org.springframework.beans.BeansException 发生错误
	 */
	Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException;

	/**
	 * 在任何bean初始化回调之后, 将此BeanPostProcessor应用于给定的新bean实例
	 * (比如InitializingBean的{@code afterPropertiesSet}或自定义init方法).
	 * bean已经填充了属性值. 返回的bean实例可能是原始实例的包装器.
	 * <p>如果是FactoryBean, 将为FactoryBean实例和FactoryBean创建的对象调用此回调 (截止 Spring 2.0).
	 * 后处理器可以通过相应的{@code bean instanceof FactoryBean}检查来决定是应用于FactoryBean, 还是创建的对象.
	 * <p>在{@link InstantiationAwareBeanPostProcessor#postProcessBeforeInstantiation}方法触发的短路之后,
	 * 也将调用此回调, 与所有其他BeanPostProcessor回调相比.
	 * 
	 * @param bean 新bean实例
	 * @param beanName bean的名称
	 * 
	 * @return 要使用的bean实例, 无论是原始的还是包装后的;
	 * 如果是{@code null}, 不会调用后续的BeanPostProcessors
	 * @throws org.springframework.beans.BeansException 发生错误
	 */
	Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException;

}
