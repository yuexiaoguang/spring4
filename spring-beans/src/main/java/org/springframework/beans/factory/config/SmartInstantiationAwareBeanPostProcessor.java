package org.springframework.beans.factory.config;

import java.lang.reflect.Constructor;

import org.springframework.beans.BeansException;

/**
 * {@link InstantiationAwareBeanPostProcessor}接口的扩展, 添加回调以预测已处理的bean的最终类型.
 *
 * <p><b>NOTE:</b> 此接口是一个专用接口, 主要供框架内部使用.
 * 通常, 应用程序提供的后处理器应该简单地实现普通的 {@link BeanPostProcessor}接口,
 * 或者从{@link InstantiationAwareBeanPostProcessorAdapter}类派生.
 * 即使在点发行版中, 也可能会向此接口添加新方法.
 */
public interface SmartInstantiationAwareBeanPostProcessor extends InstantiationAwareBeanPostProcessor {

	/**
	 * 预测最终从此处理器的{@link #postProcessBeforeInstantiation}回调中返回的bean的类型.
	 * 
	 * @param beanClass bean的原始类
	 * @param beanName bean的名称
	 * 
	 * @return bean的类型, 或{@code null}
	 * @throws org.springframework.beans.BeansException 发生错误
	 */
	Class<?> predictBeanType(Class<?> beanClass, String beanName) throws BeansException;

	/**
	 * 确定要用于给定bean的候选构造函数.
	 * 
	 * @param beanClass bean的原始类 (never {@code null})
	 * @param beanName bean的名称
	 * 
	 * @return 候选构造函数, 或{@code null}如果未指定
	 * @throws org.springframework.beans.BeansException 发生错误
	 */
	Constructor<?>[] determineCandidateConstructors(Class<?> beanClass, String beanName) throws BeansException;

	/**
	 * 获取早期访问指定bean的引用, 通常用于解析循环引用.
	 * <p>此回调使后处理器有机会提前公开包装器 - 也就是说, 在目标bean实例完全初始化之前.
	 * 暴露的对象应该等同于
	 * {@link #postProcessBeforeInitialization} / {@link #postProcessAfterInitialization},
	 * 否则将公开.
	 * 请注意, 此方法返回的对象将用作bean引用, 除非后处理器从后处理回调中返回不同的包装器.
	 * 换句话说: 那些后处理回调可能最终公开相同的引用, 或者从后续的回调中返回原始bean实例
	 * (如果已经构建受影响的bean的包装器, 用于调用此方法, 则默认情况下它将作为final bean引用公开).
	 * 
	 * @param bean 原始bean实例
	 * @param beanName bean的名称
	 * 
	 * @return 要作为bean引用公开的对象 (通常使用传入的bean实例作为默认值)
	 * @throws org.springframework.beans.BeansException 发生错误
	 */
	Object getEarlyBeanReference(Object bean, String beanName) throws BeansException;

}
