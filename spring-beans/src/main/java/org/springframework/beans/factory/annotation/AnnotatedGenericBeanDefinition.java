package org.springframework.beans.factory.annotation;

import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.util.Assert;

/**
 * 扩展{@link org.springframework.beans.factory.support.GenericBeanDefinition}类,
 * 添加对通过{@link AnnotatedBeanDefinition}接口公开的注解元数据的支持.
 *
 * <p>此GenericBeanDefinition变体主要用于测试期望在AnnotatedBeanDefinition上操作的代码,
 * 例如Spring的组件扫描支持中的策略实现 (默认定义类是
 * {@link org.springframework.context.annotation.ScannedGenericBeanDefinition},
 * 也实现了AnnotatedBeanDefinition 接口).
 */
@SuppressWarnings("serial")
public class AnnotatedGenericBeanDefinition extends GenericBeanDefinition implements AnnotatedBeanDefinition {

	private final AnnotationMetadata metadata;

	private MethodMetadata factoryMethodMetadata;


	/**
	 * @param beanClass 加载的bean 类
	 */
	public AnnotatedGenericBeanDefinition(Class<?> beanClass) {
		setBeanClass(beanClass);
		this.metadata = new StandardAnnotationMetadata(beanClass, true);
	}

	/**
	 * 为给定的注解元数据创建新的AnnotatedGenericBeanDefinition, 允许基于ASM的处理并避免过早加载bean类.
	 * 请注意, 此构造函数在功能上等效于
	 * {@link org.springframework.context.annotation.ScannedGenericBeanDefinition ScannedGenericBeanDefinition},
	 * 然而后者的语义表明, 与其他方法相比, 特殊的是通过组件扫描发现bean.
	 * 
	 * @param metadata 有问题的bean类的注解元数据
	 * @since 3.1.1
	 */
	public AnnotatedGenericBeanDefinition(AnnotationMetadata metadata) {
		Assert.notNull(metadata, "AnnotationMetadata must not be null");
		if (metadata instanceof StandardAnnotationMetadata) {
			setBeanClass(((StandardAnnotationMetadata) metadata).getIntrospectedClass());
		}
		else {
			setBeanClassName(metadata.getClassName());
		}
		this.metadata = metadata;
	}

	/**
	 * 为给定的注解元数据创建新的AnnotatedGenericBeanDefinition, 基于该类的带注解的类和工厂方法.
	 * 
	 * @param metadata 有问题的bean类的注解元数据
	 * @param factoryMethodMetadata 所选工厂方法的元数据
	 * 
	 * @since 4.1.1
	 */
	public AnnotatedGenericBeanDefinition(AnnotationMetadata metadata, MethodMetadata factoryMethodMetadata) {
		this(metadata);
		Assert.notNull(factoryMethodMetadata, "MethodMetadata must not be null");
		setFactoryMethodName(factoryMethodMetadata.getMethodName());
		this.factoryMethodMetadata = factoryMethodMetadata;
	}


	@Override
	public final AnnotationMetadata getMetadata() {
		 return this.metadata;
	}

	@Override
	public final MethodMetadata getFactoryMethodMetadata() {
		return this.factoryMethodMetadata;
	}

}
