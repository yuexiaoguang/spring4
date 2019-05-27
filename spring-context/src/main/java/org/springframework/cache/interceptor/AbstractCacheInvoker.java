package org.springframework.cache.interceptor;

import org.springframework.cache.Cache;
import org.springframework.util.Assert;

/**
 * 用于调用{@link Cache}操作, 并在发生异常时使用可配置的{@link CacheErrorHandler}的基本组件.
 */
public abstract class AbstractCacheInvoker {

	private CacheErrorHandler errorHandler;


	protected AbstractCacheInvoker() {
		this(new SimpleCacheErrorHandler());
	}

	protected AbstractCacheInvoker(CacheErrorHandler errorHandler) {
		Assert.notNull(errorHandler, "ErrorHandler must not be null");
		this.errorHandler = errorHandler;
	}


	/**
	 * 设置{@link CacheErrorHandler}实例, 以用于处理缓存提供程序引发的错误.
	 * 默认情况下, 使用{@link SimpleCacheErrorHandler}, 它会抛出任何异常.
	 */
	public void setErrorHandler(CacheErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	/**
	 * 返回要使用的{@link CacheErrorHandler}.
	 */
	public CacheErrorHandler getErrorHandler() {
		return this.errorHandler;
	}


	/**
	 * 在指定的{@link Cache}上执行{@link Cache#get(Object)}, 并在发生异常时调用错误处理程序.
	 * 如果处理程序没有抛出任何异常, 则返回{@code null}, 这会在出现错误时模拟缓存未命中.
	 */
	protected Cache.ValueWrapper doGet(Cache cache, Object key) {
		try {
			return cache.get(key);
		}
		catch (RuntimeException ex) {
			getErrorHandler().handleCacheGetError(ex, cache, key);
			return null;  // 如果处理异常, 则返回缓存未命中
		}
	}

	/**
	 * 在指定的{@link Cache}上执行{@link Cache#put(Object, Object)}, 并在发生异常时调用错误处理程序.
	 */
	protected void doPut(Cache cache, Object key, Object result) {
		try {
			cache.put(key, result);
		}
		catch (RuntimeException ex) {
			getErrorHandler().handleCachePutError(ex, cache, key, result);
		}
	}

	/**
	 * 在指定的{@link Cache}上执行{@link Cache#evict(Object)}, 并在发生异常时调用错误处理程序.
	 */
	protected void doEvict(Cache cache, Object key) {
		try {
			cache.evict(key);
		}
		catch (RuntimeException ex) {
			getErrorHandler().handleCacheEvictError(ex, cache, key);
		}
	}

	/**
	 * 在指定的{@link Cache}上执行{@link Cache#clear()}, 并在发生异常时调用错误处理程序.
	 */
	protected void doClear(Cache cache) {
		try {
			cache.clear();
		}
		catch (RuntimeException ex) {
			getErrorHandler().handleCacheClearError(ex, cache);
		}
	}

}
