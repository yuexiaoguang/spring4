package org.springframework.cache.interceptor;

import org.springframework.cache.Cache;

/**
 * 简单的{@link CacheErrorHandler}, 根本不处理异常, 只是把它抛回客户端.
 */
public class SimpleCacheErrorHandler implements CacheErrorHandler {

	@Override
	public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
		throw exception;
	}

	@Override
	public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
		throw exception;
	}

	@Override
	public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
		throw exception;
	}

	@Override
	public void handleCacheClearError(RuntimeException exception, Cache cache) {
		throw exception;
	}
}
