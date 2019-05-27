package org.springframework.cache.interceptor;

import java.util.Collection;

import org.springframework.cache.Cache;

/**
 * 确定用于拦截的方法调用的{@link Cache}实例.
 *
 * <p>实现必须是线程安全的.
 */
public interface CacheResolver {

	/**
	 * 返回用于指定调用的缓存.
	 * 
	 * @param context 特定调用的上下文
	 * 
	 * @return 要使用的缓存 (never {@code null})
	 * @throws IllegalStateException 如果缓存解析失败
	 */
	Collection<? extends Cache> resolveCaches(CacheOperationInvocationContext<?> context);

}
