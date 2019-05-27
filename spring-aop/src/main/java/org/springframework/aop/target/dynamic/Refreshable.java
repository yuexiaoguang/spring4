package org.springframework.aop.target.dynamic;

/**
 * 由动态目标对象实现的接口, 以支持重新加载和可选的轮询更新.
 */
public interface Refreshable {

	/**
	 * 刷新底层目标对象.
	 */
	void refresh();

	/**
	 * 返回自启动以来的实际刷新次数.
	 */
	long getRefreshCount();

	/**
	 * 返回上次实际刷新发生的时间 (as timestamp).
	 */
	long getLastRefreshTime();

}
