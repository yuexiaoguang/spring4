package org.springframework.cache.interceptor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.util.Assert;

/**
 * 基础{@link CacheResolver}实现, 要求具体实现基于调用上下文提供缓存名称的集合.
 */
public abstract class AbstractCacheResolver implements CacheResolver, InitializingBean {

	private CacheManager cacheManager;


	protected AbstractCacheResolver() {
	}

	/**
	 * @param cacheManager 要使用的CacheManager
	 */
	protected AbstractCacheResolver(CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}


	/**
	 * 设置此实例应使用的{@link CacheManager}.
	 */
	public void setCacheManager(CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}

	/**
	 * 返回此实例应使用的{@link CacheManager}.
	 */
	public CacheManager getCacheManager() {
		return this.cacheManager;
	}

	@Override
	public void afterPropertiesSet()  {
		Assert.notNull(this.cacheManager, "CacheManager is required");
	}


	@Override
	public Collection<? extends Cache> resolveCaches(CacheOperationInvocationContext<?> context) {
		Collection<String> cacheNames = getCacheNames(context);
		if (cacheNames == null) {
			return Collections.emptyList();
		}
		Collection<Cache> result = new ArrayList<Cache>(cacheNames.size());
		for (String cacheName : cacheNames) {
			Cache cache = getCacheManager().getCache(cacheName);
			if (cache == null) {
				throw new IllegalArgumentException("Cannot find cache named '" +
						cacheName + "' for " + context.getOperation());
			}
			result.add(cache);
		}
		return result;
	}

	/**
	 * 提供要针对当前缓存管理器解析的缓存的名称.
	 * <p>可以返回{@code null}以指示无法为此调用解析缓存.
	 * 
	 * @param context 特定调用的上下文
	 * 
	 * @return 要解析的缓存名称; 如果不应解析缓存, 则为{@code null}
	 */
	protected abstract Collection<String> getCacheNames(CacheOperationInvocationContext<?> context);

}
