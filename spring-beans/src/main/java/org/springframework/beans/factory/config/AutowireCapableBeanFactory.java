package org.springframework.beans.factory.config;

import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;

/**
 * {@link org.springframework.beans.factory.BeanFactory}接口的扩展由能够自动装配的bean工厂实现,
 * 只要他们想要为现有的bean实例公开此功能.
 *
 * <p>BeanFactory的这个子接口并不适用于普通的应用程序代码:
 * 坚持{@link org.springframework.beans.factory.BeanFactory}
 * 或{@link org.springframework.beans.factory.ListableBeanFactory}的典型应用案例.
 *
 * <p>其他框架的集成代码可以利用此接口来连接和填充Spring无法控制其生命周期的现有Bean实例.
 * 例如, 这对WebWork Actions和Tapestry Page对象特别有用.
 *
 * <p>请注意, 此接口不是由{@link org.springframework.context.ApplicationContext}外观实现的,
 * 因为它几乎没有被应用程序代码使用.
 * 那就是说, 它也可以从应用程序上下文中获得, 通过ApplicationContext的
 * {@link org.springframework.context.ApplicationContext#getAutowireCapableBeanFactory()}方法.
 *
 * <p>也可以实现 {@link org.springframework.beans.factory.BeanFactoryAware}接口,
 * 即使在ApplicationContext中运行, 它也会暴露内部BeanFactory, 访问AutowireCapableBeanFactory:
 * 简单地将传入的BeanFactory强制转换为AutowireCapableBeanFactory.
 */
public interface AutowireCapableBeanFactory extends BeanFactory {

	/**
	 * 没有外部定义的自动装配.
	 * 请注意, 仍将应用BeanFactoryAware等和注解驱动的注入.
	 */
	int AUTOWIRE_NO = 0;

	/**
	 * 按名称自动装配bean属性 (适用于所有bean属性setter).
	 */
	int AUTOWIRE_BY_NAME = 1;

	/**
	 * 按类型自动装配bean属性 (适用于所有bean属性 setter).
	 */
	int AUTOWIRE_BY_TYPE = 2;

	/**
	 * 自动装配可以满足的最贪婪的构造函数 (调用解析适当的构造函数).
	 */
	int AUTOWIRE_CONSTRUCTOR = 3;

	/**
	 * 通过反射bean类确定适当的自动装配策略.
	 * 
	 * @deprecated as of Spring 3.0: If you are using mixed autowiring strategies,
	 * prefer annotation-based autowiring for clearer demarcation of autowiring needs.
	 */
	@Deprecated
	int AUTOWIRE_AUTODETECT = 4;


	//-------------------------------------------------------------------------
	// Typical methods for creating and populating external bean instances
	//-------------------------------------------------------------------------

	/**
	 * 完全创建给定类的新bean实例.
	 * <p>执行bean的完全初始化, 包括所有适用的 {@link BeanPostProcessor BeanPostProcessors}.
	 * <p>Note: 这用于创建新实例, 填充带注解的字段和方法以及应用所有标准bean初始化回调.
	 * 它并不意味着传统的按名称或按类型自动装配属性;
	 * 使用{@link #createBean(Class, int, boolean)}用于这些目的.
	 * 
	 * @param beanClass 要创建的bean的类
	 * 
	 * @return 新的bean实例
	 * @throws BeansException 如果实例化或装配失败
	 */
	<T> T createBean(Class<T> beanClass) throws BeansException;

	/**
	 * 通过应用after-instantiation回调和bean属性后处理来填充给定的bean实例 (e.g. 注解驱动的注入).
	 * <p>Note: 这主要用于（重新）填充带注解的字段和方法, 用于新实例或反序列化实例.
	 * 它并不意味着传统的按名称或按类型自动装配属性; 使用{@link #autowireBeanProperties} 用于这些目的.
	 * 
	 * @param existingBean 现有的bean实例
	 * 
	 * @throws BeansException 如果装配失败
	 */
	void autowireBean(Object existingBean) throws BeansException;

	/**
	 * 配置给定的原始bean: 自动装配bean属性, 应用bean属性值,
	 * 应用工厂回调例如 {@code setBeanName}和{@code setBeanFactory}, 并且还应用所有bean后处理器
	 * (包括可能包装给定原始bean的那些).
	 * <p>这实际上是{@link #initializeBean}提供的超集, 完全应用相应bean定义指定的配置.
	 * <b>Note: 此方法需要给定名称的bean定义!</b>
	 * 
	 * @param existingBean 现有的bean实例
	 * @param beanName 要传递给它的bean的名称 (必须提供该名称的bean定义)
	 * 
	 * @return 要使用的bean实例, 无论是原始的还是包装后的
	 * @throws org.springframework.beans.factory.NoSuchBeanDefinitionException 如果没有给定名称的bean定义
	 * @throws BeansException 如果初始化失败
	 */
	Object configureBean(Object existingBean, String beanName) throws BeansException;


	//-------------------------------------------------------------------------
	// Specialized methods for fine-grained control over the bean lifecycle
	//-------------------------------------------------------------------------

	/**
	 * 使用指定的autowire策略完全创建给定类的新bean实例.
	 * 此处支持此接口中定义的所有常量.
	 * <p>执行bean的完全初始化, 包括所有应用的 {@link BeanPostProcessor BeanPostProcessors}.
	 * 这实际上是{@link #autowire}提供的超集, 添加{@link #initializeBean}行为.
	 * 
	 * @param beanClass 要创建的bean的类
	 * @param autowireMode 按名称或类型, 使用此接口中的常量
	 * @param dependencyCheck 是否对对象执行依赖性检查 (不适用于自动装配构造函数, 因此忽略它)
	 * 
	 * @return 新bean实例
	 * @throws BeansException 如果实例化或装配失败
	 */
	Object createBean(Class<?> beanClass, int autowireMode, boolean dependencyCheck) throws BeansException;

	/**
	 * 使用指定的autowire策略实例化给定类的新bean实例.
	 * 此处支持此接口中定义的所有常量.
	 * 也可以使用{@code AUTOWIRE_NO}调用, 以便只应用before-instantiation回调 (e.g. 用于注解驱动的注入).
	 * <p>不应用标准的{@link BeanPostProcessor BeanPostProcessors}回调或执行bean的进一步初始化.
	 * 此接口为这些目的提供了独特的细粒度操作, 例如 {@link #initializeBean}.
	 * 但是, 应用了{@link InstantiationAwareBeanPostProcessor}回调, 如果适用于实例的构造.
	 * 
	 * @param beanClass 要实例化的bean的类
	 * @param autowireMode 按名称或类型, 使用此接口中的常量
	 * @param dependencyCheck 是否对对象执行依赖性检查 (不适用于自动装配构造函数, 因此忽略它)
	 * 
	 * @return 新bean实例
	 * @throws BeansException 如果实例化或装配失败
	 */
	Object autowire(Class<?> beanClass, int autowireMode, boolean dependencyCheck) throws BeansException;

	/**
	 * 按名称或类型自动装配给定bean实例的bean属性.
	 * 也可以使用{@code AUTOWIRE_NO}调用, 以便只应用 after-instantiation 回调 (e.g. 用于注解驱动的注入).
	 * <p>不应用标准的{@link BeanPostProcessor BeanPostProcessors}回调或执行bean的进一步初始化.
	 * 此接口为这些目的提供了独特的细粒度操作, 例如 {@link #initializeBean}.
	 * 但是, 应用了{@link InstantiationAwareBeanPostProcessor}回调, 如果适用于实例的构造.
	 * 
	 * @param existingBean 要实例化的bean的类
	 * @param autowireMode 按名称或类型, 使用此接口中的常量
	 * @param dependencyCheck 是否对bean实例中的对象引用执行依赖性检查
	 * 
	 * @throws BeansException 如果装配失败
	 */
	void autowireBeanProperties(Object existingBean, int autowireMode, boolean dependencyCheck)
			throws BeansException;

	/**
	 * 将具有给定名称的bean定义的属性值应用于给定的bean实例.
	 * bean定义可以定义一个完全独立的bean, 重用其属性值, 或仅用于现有bean实例的属性值.
	 * <p>此方法不会自动装配bean属性; 它只是应用明确定义的属性值.
	 * 使用{@link #autowireBeanProperties}方法自动装配现有的bean实例.
	 * <b>Note: 此方法需要给定名称的bean定义!</b>
	 * <p>不应用标准的{@link BeanPostProcessor BeanPostProcessors}回调或执行bean的进一步初始化.
	 * 此接口为这些目的提供了独特的细粒度操作, 例如 {@link #initializeBean}.
	 * 但是, 应用了{@link InstantiationAwareBeanPostProcessor}回调, 如果适用于实例的构造.
	 * 
	 * @param existingBean 现有的bean实例
	 * @param beanName bean工厂中bean定义的名称(必须提供该名称的bean定义)
	 * 
	 * @throws org.springframework.beans.factory.NoSuchBeanDefinitionException 如果没有给定名称的bean定义
	 * @throws BeansException 如果应用属性值失败
	 */
	void applyBeanPropertyValues(Object existingBean, String beanName) throws BeansException;

	/**
	 * 初始化给定的原始bean, 应用工厂回调, 例如 {@code setBeanName}和{@code setBeanFactory},
	 * 还应用所有bean后处理器 (包括可能包装给定的原始bean的那些).
	 * <p>请注意, bean工厂中不必存在给定名称的bean定义.
	 * 传入的bean名称将仅用于回调, 但不会根据已注册的bean定义进行检查.
	 * 
	 * @param existingBean 现有的bean实例
	 * @param beanName 要传递给它的bean的名称 (只传递给 {@link BeanPostProcessor BeanPostProcessors})
	 * 
	 * @return 要使用的bean实例, 无论是原始的还是包装后的
	 * @throws BeansException 如果初始化失败
	 */
	Object initializeBean(Object existingBean, String beanName) throws BeansException;

	/**
	 * 将{@link BeanPostProcessor BeanPostProcessors}应用于给定的现有bean实例, 调用它们的 {@code postProcessBeforeInitialization}方法.
	 * 返回的bean实例可能是原始实例的包装器.
	 * 
	 * @param existingBean 新的bean实例
	 * @param beanName bean的名称
	 * 
	 * @return 要使用的bean实例, 无论是原始的还是包装后的
	 * @throws BeansException 如果后处理失败
	 */
	Object applyBeanPostProcessorsBeforeInitialization(Object existingBean, String beanName)
			throws BeansException;

	/**
	 * 将{@link BeanPostProcessor BeanPostProcessors}应用于给定的现有bean实例, 调用它们的{@code postProcessAfterInitialization}方法.
	 * 返回的bean实例可能是原始实例的包装器.
	 * 
	 * @param existingBean 新的bean实例
	 * @param beanName bean的名称
	 * 
	 * @return 要使用的bean实例, 无论是原始的还是包装后的
	 * @throws BeansException 如果后处理失败
	 */
	Object applyBeanPostProcessorsAfterInitialization(Object existingBean, String beanName)
			throws BeansException;

	/**
	 * 销毁给定的bean实例 (通常来自{@link #createBean}),
	 * 应用 {@link org.springframework.beans.factory.DisposableBean}约定,
	 * 以及已注册的{@link DestructionAwareBeanPostProcessor DestructionAwareBeanPostProcessors}.
	 * <p>应该捕获并记录在销毁期间出现的任何异常, 而不是传播给此方法的调用方.
	 * 
	 * @param existingBean 要销毁的bean实例
	 */
	void destroyBean(Object existingBean);


	//-------------------------------------------------------------------------
	// Delegate methods for resolving injection points
	//-------------------------------------------------------------------------

	/**
	 * 解析与给定对象类型唯一匹配的bean实例, 包括其bean名称.
	 * <p>这实际上是{@link #getBean(Class)}的变体, 它保留匹配实例的bean名称.
	 * 
	 * @param requiredType 必须匹配的 bean类型; 可以是接口或超类. 不允许{@code null}.
	 * 
	 * @return bean名称加上bean实例
	 * @throws NoSuchBeanDefinitionException 如果没有找到匹配的bean
	 * @throws NoUniqueBeanDefinitionException 如果找到多个匹配的bean
	 * @throws BeansException 如果无法创建bean
	 */
	<T> NamedBeanHolder<T> resolveNamedBean(Class<T> requiredType) throws BeansException;

	/**
	 * 针对此工厂中定义的bean解析指定的依赖项.
	 * 
	 * @param descriptor 依赖项的描述符 (field/method/constructor)
	 * @param requestingBeanName 声明给定的依赖项的bean的名称
	 * 
	 * @return 已解析的对象, 或{@code null}
	 * @throws NoSuchBeanDefinitionException 如果没有找到匹配的bean
	 * @throws NoUniqueBeanDefinitionException 如果找到多个匹配的bean
	 * @throws BeansException 如果依赖项解析因任何其他原因而失败
	 */
	Object resolveDependency(DependencyDescriptor descriptor, String requestingBeanName) throws BeansException;

	/**
	 * 针对此工厂中定义的bean解析指定的依赖项.
	 * 
	 * @param descriptor 依赖项的描述符 (field/method/constructor)
	 * @param requestingBeanName 声明给定的依赖项的bean的名称
	 * @param autowiredBeanNames 应该添加自动装配的bean的所有名称(用于解析给定的依赖关系)
	 * @param typeConverter 用于填充数组和集合
	 * 
	 * @return 已解析的对象, 或{@code null}
	 * @throws NoSuchBeanDefinitionException 如果没有找到匹配的bean
	 * @throws NoUniqueBeanDefinitionException 如果找到多个匹配的bean
	 * @throws BeansException 如果依赖项解析因任何其他原因而失败
	 */
	Object resolveDependency(DependencyDescriptor descriptor, String requestingBeanName,
			Set<String> autowiredBeanNames, TypeConverter typeConverter) throws BeansException;

}
