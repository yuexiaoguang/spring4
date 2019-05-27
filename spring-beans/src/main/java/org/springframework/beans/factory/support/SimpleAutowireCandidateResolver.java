package org.springframework.beans.factory.support;

import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.DependencyDescriptor;

/**
 * {@link AutowireCandidateResolver} 实现, 在没有可用的注解支持时使用.
 * 此实现仅检查bean定义.
 */
public class SimpleAutowireCandidateResolver implements AutowireCandidateResolver {

	@Override
	public boolean isAutowireCandidate(BeanDefinitionHolder bdHolder, DependencyDescriptor descriptor) {
		return bdHolder.getBeanDefinition().isAutowireCandidate();
	}

	/**
	 * 确定是否要求给定的描述符必需.
	 * <p>默认实现检查 {@link DependencyDescriptor#isRequired()}.
	 * 
	 * @param descriptor 目标方法参数或字段的描述符
	 * 
	 * @return 是否将描述符标记为必需, 或可能以其他方式指示非必需状态 (e.g. 通过参数注解)
	 */
	public boolean isRequired(DependencyDescriptor descriptor) {
		return descriptor.isRequired();
	}

	@Override
	public Object getSuggestedValue(DependencyDescriptor descriptor) {
		return null;
	}

	@Override
	public Object getLazyResolutionProxyIfNecessary(DependencyDescriptor descriptor, String beanName) {
		return null;
	}

}
