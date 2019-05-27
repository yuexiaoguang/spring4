package org.springframework.aop.target;

/**
 * 用于池化目标源的Config接口.
 */
public interface PoolingConfig {

	/**
	 * 返回池的最大大小.
	 */
	int getMaxSize();

	/**
	 * 返回池中活动对象的数量.
	 * 
	 * @throws UnsupportedOperationException 如果池不支持
	 */
	int getActiveCount() throws UnsupportedOperationException;

	/**
	 * 返回池中的空闲对象数.
	 * 
	 * @throws UnsupportedOperationException 如果池不支持
	 */
	int getIdleCount() throws UnsupportedOperationException;

}
