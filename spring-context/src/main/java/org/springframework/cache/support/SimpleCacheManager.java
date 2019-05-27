package org.springframework.cache.support;

import java.util.Collection;
import java.util.Collections;

import org.springframework.cache.Cache;

/**
 * 针对给定的缓存集合的简单缓存管理器.
 * 用于测试或简单缓存声明.
 */
public class SimpleCacheManager extends AbstractCacheManager {

	private Collection<? extends Cache> caches = Collections.emptySet();


	/**
	 * 指定要用于此CacheManager的Cache实例的集合.
	 */
	public void setCaches(Collection<? extends Cache> caches) {
		this.caches = caches;
	}

	@Override
	protected Collection<? extends Cache> loadCaches() {
		return this.caches;
	}

}
