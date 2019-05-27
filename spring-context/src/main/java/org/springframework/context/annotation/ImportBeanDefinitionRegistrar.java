package org.springframework.context.annotation;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.core.type.AnnotationMetadata;

/**
 * 由在处理@{@link Configuration}类时注册其他bean定义的类型实现的接口.
 * 当需要在bean定义级别(与{@code @Bean}方法/实例级别相反)操作时, 是有用的.
 *
 * <p>与{@code @Configuration}和{@link ImportSelector}一起, 此类型的类可以提供给@{@link Import}注解
 * (或者也可以从{@code ImportSelector}返回).
 *
 * <p>{@link ImportBeanDefinitionRegistrar}可以实现以下任何{@link org.springframework.beans.factory.Aware Aware}接口,
 * 并在{@link #registerBeanDefinitions}之前调用它们各自的方法:
 * <ul>
 * <li>{@link org.springframework.context.EnvironmentAware EnvironmentAware}</li>
 * <li>{@link org.springframework.beans.factory.BeanFactoryAware BeanFactoryAware}
 * <li>{@link org.springframework.beans.factory.BeanClassLoaderAware BeanClassLoaderAware}
 * <li>{@link org.springframework.context.ResourceLoaderAware ResourceLoaderAware}
 * </ul>
 *
 * <p>有关用法示例, 请参阅实现和相关的单元测试.
 */
public interface ImportBeanDefinitionRegistrar {

	/**
	 * 基于导入的{@code @Configuration}类的给定注解元数据, 根据需要注册bean定义.
	 * <p>请注意, 此处可能未注册{@link BeanDefinitionRegistryPostProcessor}类型,
	 * 由于与{@code @Configuration}类处理相关的生命周期约束.
	 * 
	 * @param importingClassMetadata 导入类的注解元数据
	 * @param registry 当前bean定义注册表
	 */
	public void registerBeanDefinitions(
			AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry);

}
