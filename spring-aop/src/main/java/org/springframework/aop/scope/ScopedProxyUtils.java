package org.springframework.aop.scope;

import org.springframework.aop.framework.autoproxy.AutoProxyUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;

/**
 * 用于创建作用域代理的实用程序类.
 * 由ScopedProxyBeanDefinitionDecorator和ClassPathBeanDefinitionScanner使用.
 */
public abstract class ScopedProxyUtils {

	private static final String TARGET_NAME_PREFIX = "scopedTarget.";


	/**
	 * 为提供的目标bean生成作用域代理, 使用内部名称注册目标bean并在作用域代理上设置“targetBeanName”.
	 * 
	 * @param definition 原始的bean定义
	 * @param registry bean定义注册表
	 * @param proxyTargetClass 是否创建目标类代理
	 * 
	 * @return 作用域代理定义
	 */
	public static BeanDefinitionHolder createScopedProxy(BeanDefinitionHolder definition,
			BeanDefinitionRegistry registry, boolean proxyTargetClass) {

		String originalBeanName = definition.getBeanName();
		BeanDefinition targetDefinition = definition.getBeanDefinition();
		String targetBeanName = getTargetBeanName(originalBeanName);

		// 为原始bean名称创建作用域代理定义, 将目标bean“隐藏”在内部目标定义中.
		RootBeanDefinition proxyDefinition = new RootBeanDefinition(ScopedProxyFactoryBean.class);
		proxyDefinition.setDecoratedDefinition(new BeanDefinitionHolder(targetDefinition, targetBeanName));
		proxyDefinition.setOriginatingBeanDefinition(targetDefinition);
		proxyDefinition.setSource(definition.getSource());
		proxyDefinition.setRole(targetDefinition.getRole());

		proxyDefinition.getPropertyValues().add("targetBeanName", targetBeanName);
		if (proxyTargetClass) {
			targetDefinition.setAttribute(AutoProxyUtils.PRESERVE_TARGET_CLASS_ATTRIBUTE, Boolean.TRUE);
			// ScopedProxyFactoryBean's "proxyTargetClass" default is TRUE, 所以我们不需要在这里明确设置它.
		}
		else {
			proxyDefinition.getPropertyValues().add("proxyTargetClass", Boolean.FALSE);
		}

		// 从原始bean定义复制autowire设置.
		proxyDefinition.setAutowireCandidate(targetDefinition.isAutowireCandidate());
		proxyDefinition.setPrimary(targetDefinition.isPrimary());
		if (targetDefinition instanceof AbstractBeanDefinition) {
			proxyDefinition.copyQualifiersFrom((AbstractBeanDefinition) targetDefinition);
		}

		// 应该忽略目标bean，以支持作用域代理.
		targetDefinition.setAutowireCandidate(false);
		targetDefinition.setPrimary(false);

		// 在工厂中将目标bean注册为单独的bean.
		registry.registerBeanDefinition(targetBeanName, targetDefinition);

		// 将作用域代理定义作为主bean定义返回 (可能是一个内部 bean).
		return new BeanDefinitionHolder(proxyDefinition, originalBeanName, definition.getAliases());
	}

	/**
	 * 生成在作用域代理中使用的bean名称以引用目标bean.
	 * 
	 * @param originalBeanName bean的原始名称
	 * 
	 * @return 生成的bean用于引用目标bean
	 */
	public static String getTargetBeanName(String originalBeanName) {
		return TARGET_NAME_PREFIX + originalBeanName;
	}

	/**
	 * 指定{@code beanName}是否是引用作用域代理中的目标bean的bean的名称.
	 * 
	 * @since 4.1.4
	 */
	public static boolean isScopedTarget(String beanName) {
		return (beanName != null && beanName.startsWith(TARGET_NAME_PREFIX));
	}

}
