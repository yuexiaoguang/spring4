package org.springframework.aop.framework.autoproxy;

import org.springframework.core.NamedThreadLocal;

/**
 * 当前代理创建上下文的持有者, 由自动代理创建者公开, 例如{@link AbstractAdvisorAutoProxyCreator}.
 */
public class ProxyCreationContext {

	/** 在Advisor匹配期间持有当前代理bean名称的ThreadLocal */
	private static final ThreadLocal<String> currentProxiedBeanName =
			new NamedThreadLocal<String>("Name of currently proxied bean");


	/**
	 * 返回当前代理的bean实例的名称.
	 * 
	 * @return bean的名称, 或 {@code null}
	 */
	public static String getCurrentProxiedBeanName() {
		return currentProxiedBeanName.get();
	}

	/**
	 * 设置当前代理的bean实例的名称.
	 * 
	 * @param beanName bean的名称, 或 {@code null}重置它
	 */
	static void setCurrentProxiedBeanName(String beanName) {
		if (beanName != null) {
			currentProxiedBeanName.set(beanName);
		}
		else {
			currentProxiedBeanName.remove();
		}
	}

}
