package org.springframework.cache.ehcache;

import java.util.Collection;
import java.util.LinkedHashSet;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Status;

import org.springframework.cache.Cache;
import org.springframework.cache.transaction.AbstractTransactionSupportingCacheManager;

/**
 * 由EhCache支持的CacheManager {@link net.sf.ehcache.CacheManager}.
 */
public class EhCacheCacheManager extends AbstractTransactionSupportingCacheManager {

	private net.sf.ehcache.CacheManager cacheManager;


	/**
	 * 创建一个新的EhCacheCacheManager, 通过{@link #setCacheManager} bean属性设置目标EhCache CacheManager.
	 */
	public EhCacheCacheManager() {
	}

	/**
	 * 为给定的后备EhCache CacheManager创建一个新的EhCacheCacheManager.
	 * 
	 * @param cacheManager 支持的EhCache {@link net.sf.ehcache.CacheManager}
	 */
	public EhCacheCacheManager(net.sf.ehcache.CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}


	/**
	 * 设置支持的EhCache {@link net.sf.ehcache.CacheManager}.
	 */
	public void setCacheManager(net.sf.ehcache.CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}

	/**
	 * 返回支持的EhCache {@link net.sf.ehcache.CacheManager}.
	 */
	public net.sf.ehcache.CacheManager getCacheManager() {
		return this.cacheManager;
	}

	@Override
	public void afterPropertiesSet() {
		if (getCacheManager() == null) {
			setCacheManager(EhCacheManagerUtils.buildCacheManager());
		}
		super.afterPropertiesSet();
	}


	@Override
	protected Collection<Cache> loadCaches() {
		Status status = getCacheManager().getStatus();
		if (!Status.STATUS_ALIVE.equals(status)) {
			throw new IllegalStateException(
					"An 'alive' EhCache CacheManager is required - current cache is " + status.toString());
		}

		String[] names = getCacheManager().getCacheNames();
		Collection<Cache> caches = new LinkedHashSet<Cache>(names.length);
		for (String name : names) {
			caches.add(new EhCacheCache(getCacheManager().getEhcache(name)));
		}
		return caches;
	}

	@Override
	protected Cache getMissingCache(String name) {
		// 再次检查EhCache缓存 (如果在运行时添加了缓存)
		Ehcache ehcache = getCacheManager().getEhcache(name);
		if (ehcache != null) {
			return new EhCacheCache(ehcache);
		}
		return null;
	}

}
