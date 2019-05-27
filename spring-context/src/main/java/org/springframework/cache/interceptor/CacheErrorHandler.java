package org.springframework.cache.interceptor;

import org.springframework.cache.Cache;

/**
 * 处理缓存相关错误的策略.
 * 在大多数情况下, 提供程序抛出的任何异常都应该简单地抛回客户端,
 * 但在某些情况下, 基础结构可能需要以不同的方式处理缓存提供程序异常.
 *
 * <p>通常, 无法从具有给定id的缓存中检索对象, 可以通过不抛出此类异常来透明地管理缓存未命中.
 */
public interface CacheErrorHandler {

	/**
	 * 处理使用指定的{@code key}检索条目时缓存提供程序抛出的给定运行时异常, 可能将其重新抛出为致命异常.
	 * 
	 * @param exception 缓存提供程序抛出的异常
	 * @param cache 缓存
	 * @param key 用于获取条目的Key
	 */
	void handleCacheGetError(RuntimeException exception, Cache cache, Object key);

	/**
	 * 处理使用指定的{@code key}和{@code value}更新条目时缓存提供程序抛出的给定运行时异常, 可能会将其重新抛出为致命异常.
	 * 
	 * @param exception 缓存提供程序抛出的异常
	 * @param cache 缓存
	 * @param key 用于更新条目的Key
	 * @param value 与Key关联的值
	 */
	void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value);

	/**
	 * 处理使用指定的{@code key}清除条目时缓存提供程序抛出的给定运行时异常, 可能将其重新抛出为致命异常.
	 * 
	 * @param exception 缓存提供程序抛出的异常
	 * @param cache 缓存
	 * @param key 用于清除条目的Key
	 */
	void handleCacheEvictError(RuntimeException exception, Cache cache, Object key);

	/**
	 * 处理清除指定的{@link Cache}时缓存提供程序抛出的给定运行时异常, 可能将其重新抛出为致命异常.
	 * 
	 * @param exception 缓存提供程序抛出的异常
	 * @param cache 要清除的缓存
	 */
	void handleCacheClearError(RuntimeException exception, Cache cache);

}
