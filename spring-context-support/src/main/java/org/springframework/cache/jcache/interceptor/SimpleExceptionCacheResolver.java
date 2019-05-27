package org.springframework.cache.jcache.interceptor;

import java.util.Collection;
import java.util.Collections;

import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.AbstractCacheResolver;
import org.springframework.cache.interceptor.BasicOperation;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.CacheResolver;

/**
 * 简单的{@link CacheResolver}, 它基于可配置的{@link CacheManager}和缓存的名称来解析异常缓存:
 * {@link CacheResultOperation#getExceptionCacheName()}
 */
public class SimpleExceptionCacheResolver extends AbstractCacheResolver {

	public SimpleExceptionCacheResolver(CacheManager cacheManager) {
		super(cacheManager);
	}

	@Override
	protected Collection<String> getCacheNames(CacheOperationInvocationContext<?> context) {
		BasicOperation operation = context.getOperation();
		if (!(operation instanceof CacheResultOperation)) {
			throw new IllegalStateException("Could not extract exception cache name from " + operation);
		}
		CacheResultOperation cacheResultOperation = (CacheResultOperation) operation;
		String exceptionCacheName = cacheResultOperation.getExceptionCacheName();
		if (exceptionCacheName != null) {
			return Collections.singleton(exceptionCacheName);
		}
		return null;
	}

}
