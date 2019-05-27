package org.springframework.beans.factory.annotation;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;

/**
 * 扩展 {@link org.springframework.beans.factory.config.BeanDefinition}接口,
 * 公开关于其bean类的{@link org.springframework.core.type.AnnotationMetadata} - 不需要加载类.
 */
public interface AnnotatedBeanDefinition extends BeanDefinition {

	/**
	 * 获取此bean定义的bean类的注解元数据（以及基本类元数据）.
	 * 
	 * @return 注解元数据 (never {@code null})
	 */
	AnnotationMetadata getMetadata();

	/**
	 * 获取此bean定义的工厂方法的元数据.
	 * 
	 * @return 工厂方法的元数据, 或{@code null}
	 * @since 4.1.1
	 */
	MethodMetadata getFactoryMethodMetadata();

}
