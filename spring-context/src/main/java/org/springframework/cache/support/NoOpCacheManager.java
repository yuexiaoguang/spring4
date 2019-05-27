package org.springframework.cache.support;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

/**
 * 适用于禁用缓存的基础无操作{@link CacheManager}实现, 通常用于在没有实际后备存储的情况下备份缓存声明.
 *
 * <p>将简单地接受进入缓存的所有条目, 而不是实际存储它们.
 */
public class NoOpCacheManager implements CacheManager {

	private final ConcurrentMap<String, Cache> caches = new ConcurrentHashMap<String, Cache>(16);

	private final Set<String> cacheNames = new LinkedHashSet<String>(16);


	/**
	 * 此实现始终返回不会存储条目的{@link Cache}实现.
	 * 此外, 管理器将记住请求缓存的一致性.
	 */
	@Override
	public Cache getCache(String name) {
		Cache cache = this.caches.get(name);
		if (cache == null) {
			this.caches.putIfAbsent(name, new NoOpCache(name));
			synchronized (this.cacheNames) {
				this.cacheNames.add(name);
			}
		}

		return this.caches.get(name);
	}

	/**
	 * 此实现返回先前请求的缓存的名称.
	 */
	@Override
	public Collection<String> getCacheNames() {
		synchronized (this.cacheNames) {
			return Collections.unmodifiableSet(this.cacheNames);
		}
	}

}
