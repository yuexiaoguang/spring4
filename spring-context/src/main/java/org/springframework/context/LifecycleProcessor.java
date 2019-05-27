package org.springframework.context;

/**
 * 用于在ApplicationContext中处理Lifecycle bean的策略接口.
 */
public interface LifecycleProcessor extends Lifecycle {

	/**
	 * 上下文刷新的通知, e.g. 用于自动启动组件.
	 */
	void onRefresh();

	/**
	 * 上下文关闭阶段的通知, e.g. 用于自动关闭组件.
	 */
	void onClose();

}
