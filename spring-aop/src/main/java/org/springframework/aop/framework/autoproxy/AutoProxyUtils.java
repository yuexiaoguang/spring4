package org.springframework.aop.framework.autoproxy;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.Conventions;

/**
 * 自动代理感知组件的工具类.
 * 主要供框架内部使用.
 */
public abstract class AutoProxyUtils {

	/**
	 * Bean定义属性, 可以指示给定bean是否应该使用其目标类进行代理 (如果它首先被代理).
	 * 值是 {@code Boolean.TRUE} 或 {@code Boolean.FALSE}.
	 * <p>如果代理工厂为特定bean构建了目标类代理, 则可以设置此属性, 并希望强制将该bean转换为其目标类 (即使AOP增强通过自动代理应用).
	 */
	public static final String PRESERVE_TARGET_CLASS_ATTRIBUTE =
			Conventions.getQualifiedAttributeName(AutoProxyUtils.class, "preserveTargetClass");

	/**
	 * Bean定义属性, 指示自动代理bean的原始目标类, e.g. 用于对基于接口的代理后面的目标类的注解进行反射.
	 * 
	 * @since 4.2.3
	 */
	public static final String ORIGINAL_TARGET_CLASS_ATTRIBUTE =
			Conventions.getQualifiedAttributeName(AutoProxyUtils.class, "originalTargetClass");


	/**
	 * 确定给定的bean是否应该使用其目标类而不是其接口进行代理.
	 * 检查对应bean定义的{@link #PRESERVE_TARGET_CLASS_ATTRIBUTE "preserveTargetClass" attribute}.
	 * 
	 * @param beanFactory the containing ConfigurableListableBeanFactory
	 * @param beanName bean的名称
	 * 
	 * @return 是否应该使用其目标类代理给定的bean
	 */
	public static boolean shouldProxyTargetClass(ConfigurableListableBeanFactory beanFactory, String beanName) {
		if (beanName != null && beanFactory.containsBeanDefinition(beanName)) {
			BeanDefinition bd = beanFactory.getBeanDefinition(beanName);
			return Boolean.TRUE.equals(bd.getAttribute(PRESERVE_TARGET_CLASS_ATTRIBUTE));
		}
		return false;
	}

	/**
	 * 确定指定bean的原始目标类, 否则回到常规{@code getType}查找.
	 * 
	 * @param beanFactory the containing ConfigurableListableBeanFactory
	 * @param beanName bean的名称
	 * 
	 * @return 存储在bean定义中的原始目标类
	 * @since 4.2.3
	 */
	public static Class<?> determineTargetClass(ConfigurableListableBeanFactory beanFactory, String beanName) {
		if (beanName == null) {
			return null;
		}
		if (beanFactory.containsBeanDefinition(beanName)) {
			BeanDefinition bd = beanFactory.getMergedBeanDefinition(beanName);
			Class<?> targetClass = (Class<?>) bd.getAttribute(ORIGINAL_TARGET_CLASS_ATTRIBUTE);
			if (targetClass != null) {
				return targetClass;
			}
		}
		return beanFactory.getType(beanName);
	}

	/**
	 * 公开指定bean的给定目标类.
	 * 
	 * @param beanFactory the containing ConfigurableListableBeanFactory
	 * @param beanName bean的名称
	 * @param targetClass 相应的目标类
	 * 
	 * @since 4.2.3
	 */
	static void exposeTargetClass(ConfigurableListableBeanFactory beanFactory, String beanName, Class<?> targetClass) {
		if (beanName != null && beanFactory.containsBeanDefinition(beanName)) {
			beanFactory.getMergedBeanDefinition(beanName).setAttribute(ORIGINAL_TARGET_CLASS_ATTRIBUTE, targetClass);
		}
	}

}
