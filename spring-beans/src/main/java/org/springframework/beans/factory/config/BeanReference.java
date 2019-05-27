package org.springframework.beans.factory.config;

import org.springframework.beans.BeanMetadataElement;

/**
 * 以抽象方式公开对bean名称的引用的接口.
 * 此接口不一定意味着对实际bean实例的引用; 它只是表达了对bean名称的逻辑引用.
 *
 * <p>任何类型的bean引用持有者实现的通用接口,
 * 例如{@link RuntimeBeanReference RuntimeBeanReference}和{@link RuntimeBeanNameReference RuntimeBeanNameReference}.
 */
public interface BeanReference extends BeanMetadataElement {

	/**
	 * 返回此引用指向的目标bean名称 (never {@code null}).
	 */
	String getBeanName();

}
