package org.springframework.cache.jcache;

import java.util.Collection;
import java.util.LinkedHashSet;
import javax.cache.CacheManager;
import javax.cache.Caching;

import org.springframework.cache.Cache;
import org.springframework.cache.transaction.AbstractTransactionSupportingCacheManager;

/**
 * {@link org.springframework.cache.CacheManager}实现, 由JCache {@link javax.cache.CacheManager}支持.
 *
 * <p>Note: 从Spring 4.0开始, 此类已针对JCache 1.0进行了更新.
 */
public class JCacheCacheManager extends AbstractTransactionSupportingCacheManager {

	private javax.cache.CacheManager cacheManager;

	private boolean allowNullValues = true;


	/**
	 * 通过{@link #setCacheManager} bean属性设置目标JCache CacheManager.
	 */
	public JCacheCacheManager() {
	}

	/**
	 * @param cacheManager 支持的JCache {@link javax.cache.CacheManager}
	 */
	public JCacheCacheManager(CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}


	/**
	 * 设置支持的JCache {@link javax.cache.CacheManager}.
	 */
	public void setCacheManager(javax.cache.CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}

	/**
	 * 返回支持的JCache {@link javax.cache.CacheManager}.
	 */
	public javax.cache.CacheManager getCacheManager() {
		return this.cacheManager;
	}

	/**
	 * 指定是否接受并转换此缓存管理器中所有缓存的{@code null}值.
	 * <p>默认"true", 尽管JSR-107本身不支持{@code null}值.
	 * 内部持有者对象将用于存储用户级{@code null}.
	 */
	public void setAllowNullValues(boolean allowNullValues) {
		this.allowNullValues = allowNullValues;
	}

	/**
	 * 返回此缓存管理器是否接受并转换其所有缓存的{@code null}值.
	 */
	public boolean isAllowNullValues() {
		return this.allowNullValues;
	}

	@Override
	public void afterPropertiesSet() {
		if (getCacheManager() == null) {
			setCacheManager(Caching.getCachingProvider().getCacheManager());
		}
		super.afterPropertiesSet();
	}


	@Override
	protected Collection<Cache> loadCaches() {
		Collection<Cache> caches = new LinkedHashSet<Cache>();
		for (String cacheName : getCacheManager().getCacheNames()) {
			javax.cache.Cache<Object, Object> jcache = getCacheManager().getCache(cacheName);
			caches.add(new JCacheCache(jcache, isAllowNullValues()));
		}
		return caches;
	}

	@Override
	protected Cache getMissingCache(String name) {
		// 再次检查JCache缓存 (如果在运行时添加了缓存)
		javax.cache.Cache<Object, Object> jcache = getCacheManager().getCache(name);
		if (jcache != null) {
			return new JCacheCache(jcache, isAllowNullValues());
		}
		return null;
	}

}
