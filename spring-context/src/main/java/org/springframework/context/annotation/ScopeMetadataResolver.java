package org.springframework.context.annotation;

import org.springframework.beans.factory.config.BeanDefinition;

/**
 * 用于解析bean定义作用域的策略接口.
 */
public interface ScopeMetadataResolver {

	/**
	 * 解析适用于提供的bean {@code definition}的{@link ScopeMetadata}.
	 * <p>实现当然可以使用他们喜欢的策略来确定作用域元数据,
	 * 但是一些实现可能是使用所提供的{@code definition}的 {@link BeanDefinition#getBeanClassName() 类}上的源级别注解,
	 * 或者使用提供的{@code definition}的 {@link BeanDefinition#attributeNames()}中的元数据.
	 * 
	 * @param definition 目标bean定义
	 * 
	 * @return 相关作用域元数据; never {@code null}
	 */
	ScopeMetadata resolveScopeMetadata(BeanDefinition definition);

}
