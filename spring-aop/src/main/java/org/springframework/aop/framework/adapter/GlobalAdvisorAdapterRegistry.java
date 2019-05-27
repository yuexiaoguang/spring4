package org.springframework.aop.framework.adapter;

/**
 * 单例, 发布共享的DefaultAdvisorAdapterRegistry实例.
 */
public abstract class GlobalAdvisorAdapterRegistry {

	/**
	 * 跟踪单个实例，以便将其返回给请求它的类.
	 */
	private static AdvisorAdapterRegistry instance = new DefaultAdvisorAdapterRegistry();

	/**
	 * 返回单个 {@link DefaultAdvisorAdapterRegistry} 实例.
	 */
	public static AdvisorAdapterRegistry getInstance() {
		return instance;
	}

	/**
	 * 重置单例 {@link DefaultAdvisorAdapterRegistry},
	 * 删除所有的{@link AdvisorAdapterRegistry#registerAdvisorAdapter(AdvisorAdapter) registered}适配器.
	 */
	static void reset() {
		instance = new DefaultAdvisorAdapterRegistry();
	}

}
