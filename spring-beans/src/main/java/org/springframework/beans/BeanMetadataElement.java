package org.springframework.beans;

/**
 * 由携带配置源对象的bean元数据元素实现的接口.
 */
public interface BeanMetadataElement {

	/**
	 * 返回此元数据元素的配置源 (may be {@code null}).
	 */
	Object getSource();

}
