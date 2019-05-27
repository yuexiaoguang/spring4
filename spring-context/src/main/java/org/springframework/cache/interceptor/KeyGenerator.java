package org.springframework.cache.interceptor;

import java.lang.reflect.Method;

/**
 * 缓存key生成器. 用于基于给定方法(用作上下文)及其参数创建Key.
 */
public interface KeyGenerator {

	/**
	 * 为给定方法及其参数生成Key.
	 * 
	 * @param target 目标实例
	 * @param method 被调用的方法
	 * @param params 方法参数 (扩展了 var-args)
	 * 
	 * @return 生成的Key
	 */
	Object generate(Object target, Method method, Object... params);

}
