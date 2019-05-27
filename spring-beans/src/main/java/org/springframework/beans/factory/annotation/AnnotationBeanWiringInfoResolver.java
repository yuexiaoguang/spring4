package org.springframework.beans.factory.annotation;

import org.springframework.beans.factory.wiring.BeanWiringInfo;
import org.springframework.beans.factory.wiring.BeanWiringInfoResolver;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * 使用Configurable注解来标识哪些类需要自动装配.
 * 要查找的bean名称将从{@link Configurable}注解中获取;
 * 否则, 默认值将是要配置的类的完全限定名称.
 */
public class AnnotationBeanWiringInfoResolver implements BeanWiringInfoResolver {

	@Override
	public BeanWiringInfo resolveWiringInfo(Object beanInstance) {
		Assert.notNull(beanInstance, "Bean instance must not be null");
		Configurable annotation = beanInstance.getClass().getAnnotation(Configurable.class);
		return (annotation != null ? buildWiringInfo(beanInstance, annotation) : null);
	}

	/**
	 * 为给定的Configurable注解构建BeanWiringInfo.
	 * 
	 * @param beanInstance bean实例
	 * @param annotation 在bean类上找到的Configurable注解
	 * 
	 * @return 解析的BeanWiringInfo
	 */
	protected BeanWiringInfo buildWiringInfo(Object beanInstance, Configurable annotation) {
		if (!Autowire.NO.equals(annotation.autowire())) {
			return new BeanWiringInfo(annotation.autowire().value(), annotation.dependencyCheck());
		}
		else {
			if (!"".equals(annotation.value())) {
				// 显式指定的bean名称
				return new BeanWiringInfo(annotation.value(), false);
			}
			else {
				// default bean name
				return new BeanWiringInfo(getDefaultBeanName(beanInstance), true);
			}
		}
	}

	/**
	 * 确定指定的Bean实例的默认bean名称.
	 * <p>默认实现返回CGLIB代理的超类名称和其他普通bean类的名称.
	 * 
	 * @param beanInstance 要构建默认名称的bean实例
	 * 
	 * @return 默认bean名称
	 */
	protected String getDefaultBeanName(Object beanInstance) {
		return ClassUtils.getUserClass(beanInstance).getName();
	}

}
