package org.springframework.cache.interceptor;

import java.lang.reflect.Method;

/**
 * 表示调用缓存操作的上下文.
 *
 * <p>缓存操作是静态的, 与特定的调用无关;
 * 此接口收集操作和特定的调用.
 */
public interface CacheOperationInvocationContext<O extends BasicOperation> {

	/**
	 * 返回缓存操作.
	 */
	O getOperation();

	/**
	 * 返回调用该方法的目标实例.
	 */
	Object getTarget();

	/**
	 * 返回被调用的方法.
	 */
	Method getMethod();

	/**
	 * 返回用于调用方法的参数列表.
	 */
	Object[] getArgs();

}
