package org.springframework.cache.interceptor;

import java.util.Collection;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

/**
 * 简单的{@link CacheResolver},
 * 它基于可配置的{@link CacheManager}和 {@link BasicOperation#getCacheNames() getCacheNames()}提供的缓存名称来解析{@link Cache}实例
 */
public class SimpleCacheResolver extends AbstractCacheResolver {

	public SimpleCacheResolver() {
	}

	public SimpleCacheResolver(CacheManager cacheManager) {
		super(cacheManager);
	}

	@Override
	protected Collection<String> getCacheNames(CacheOperationInvocationContext<?> context) {
		return context.getOperation().getCacheNames();
	}

}
