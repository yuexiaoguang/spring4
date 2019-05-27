package org.springframework.context.annotation;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.util.Assert;

/**
 * 描述Spring管理的bean的作用域特征, 包括作用域名称和作用域代理行为.
 *
 * <p>默认作用域是 "singleton", 并且默认是不创建作用域代理.
 */
public class ScopeMetadata {

	private String scopeName = BeanDefinition.SCOPE_SINGLETON;

	private ScopedProxyMode scopedProxyMode = ScopedProxyMode.NO;


	/**
	 * 设置作用域的名称.
	 */
	public void setScopeName(String scopeName) {
		Assert.notNull(scopeName, "'scopeName' must not be null");
		this.scopeName = scopeName;
	}

	/**
	 * 获取作用域的名称.
	 */
	public String getScopeName() {
		return this.scopeName;
	}

	/**
	 * 设置要应用于作用域实例的代理模式.
	 */
	public void setScopedProxyMode(ScopedProxyMode scopedProxyMode) {
		Assert.notNull(scopedProxyMode, "'scopedProxyMode' must not be null");
		this.scopedProxyMode = scopedProxyMode;
	}

	/**
	 * 获取要应用于作用域实例的代理模式.
	 */
	public ScopedProxyMode getScopedProxyMode() {
		return this.scopedProxyMode;
	}

}
