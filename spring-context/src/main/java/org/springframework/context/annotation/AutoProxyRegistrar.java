package org.springframework.context.annotation;

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.config.AopConfigUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;

/**
 * 根据将{@code mode}和{@code proxyTargetClass}属性设置为正确的值的{@code @Enable*}注解,
 * 注册对应于当前{@link BeanDefinitionRegistry}的自动代理创建者.
 */
public class AutoProxyRegistrar implements ImportBeanDefinitionRegistrar {

	private final Log logger = LogFactory.getLog(getClass());

	/**
	 * 针对给定的注册表注册, 升级和配置标准自动代理创建器 (APC).
	 * 通过查找在导入{@code @Configuration}类上声明的具有{@code mode}和{@code proxyTargetClass}属性的最近注解来工作.
	 * 如果{@code mode}设置为{@code PROXY}, 则APC已注册; 如果{@code proxyTargetClass}设置为{@code true}, 则APC被强制使用子类 (CGLIB)代理.
	 * <p>多个{@code @Enable*}注解同时暴露 {@code mode}和{@code proxyTargetClass}属性.
	 * 值得注意的是, 这些功能大多数最终都会共享 {@linkplain AopConfigUtils#AUTO_PROXY_CREATOR_BEAN_NAME single APC}.
	 * 出于这个原因, 这个实现并不关心找到的注解的细节 -- 只要它暴露了正确的{@code mode}和{@code proxyTargetClass}属性, APC就可以注册和配置所有相同的.
	 */
	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		boolean candidateFound = false;
		Set<String> annoTypes = importingClassMetadata.getAnnotationTypes();
		for (String annoType : annoTypes) {
			AnnotationAttributes candidate = AnnotationConfigUtils.attributesFor(importingClassMetadata, annoType);
			if (candidate == null) {
				continue;
			}
			Object mode = candidate.get("mode");
			Object proxyTargetClass = candidate.get("proxyTargetClass");
			if (mode != null && proxyTargetClass != null && AdviceMode.class == mode.getClass() &&
					Boolean.class == proxyTargetClass.getClass()) {
				candidateFound = true;
				if (mode == AdviceMode.PROXY) {
					AopConfigUtils.registerAutoProxyCreatorIfNecessary(registry);
					if ((Boolean) proxyTargetClass) {
						AopConfigUtils.forceAutoProxyCreatorToUseClassProxying(registry);
						return;
					}
				}
			}
		}
		if (!candidateFound && logger.isWarnEnabled()) {
			String name = getClass().getSimpleName();
			logger.warn(String.format("%s was imported but no annotations were found " +
					"having both 'mode' and 'proxyTargetClass' attributes of type " +
					"AdviceMode and boolean respectively. This means that auto proxy " +
					"creator registration and configuration may not have occurred as " +
					"intended, and components may not be proxied as expected. Check to " +
					"ensure that %s has been @Import'ed on the same class where these " +
					"annotations are declared; otherwise remove the import of %s " +
					"altogether.", name, name, name));
		}
	}

}
