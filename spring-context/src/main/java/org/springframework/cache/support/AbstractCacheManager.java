package org.springframework.cache.support;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

/**
 * 实现常见{@link CacheManager}方法的抽象基类.
 * 对于缓存不会更改的“静态”环境很有用.
 */
public abstract class AbstractCacheManager implements CacheManager, InitializingBean {

	private final ConcurrentMap<String, Cache> cacheMap = new ConcurrentHashMap<String, Cache>(16);

	private volatile Set<String> cacheNames = Collections.emptySet();


	// Early cache initialization on startup

	@Override
	public void afterPropertiesSet() {
		initializeCaches();
	}

	/**
	 * 初始化缓存的静态配置.
	 * <p>通过{@link #afterPropertiesSet()}启动时触发; 也可以在运行时调用重新初始化.
	 */
	public void initializeCaches() {
		Collection<? extends Cache> caches = loadCaches();

		synchronized (this.cacheMap) {
			this.cacheNames = Collections.emptySet();
			this.cacheMap.clear();
			Set<String> cacheNames = new LinkedHashSet<String>(caches.size());
			for (Cache cache : caches) {
				String name = cache.getName();
				this.cacheMap.put(name, decorateCache(cache));
				cacheNames.add(name);
			}
			this.cacheNames = Collections.unmodifiableSet(cacheNames);
		}
	}

	/**
	 * 加载此缓存管理器的初始缓存.
	 * <p>在启动时由 {@link #afterPropertiesSet()}调用.
	 * 返回的集合可能为空, 但不能为{@code null}.
	 */
	protected abstract Collection<? extends Cache> loadCaches();


	// Lazy cache initialization on access

	@Override
	public Cache getCache(String name) {
		Cache cache = this.cacheMap.get(name);
		if (cache != null) {
			return cache;
		}
		else {
			// 现在完全同步以减少缓存创建...
			synchronized (this.cacheMap) {
				cache = this.cacheMap.get(name);
				if (cache == null) {
					cache = getMissingCache(name);
					if (cache != null) {
						cache = decorateCache(cache);
						this.cacheMap.put(name, cache);
						updateCacheNames(name);
					}
				}
				return cache;
			}
		}
	}

	@Override
	public Collection<String> getCacheNames() {
		return this.cacheNames;
	}


	// Common cache initialization delegates for subclasses

	/**
	 * 检查给定名称的已注册的缓存.
	 * 与{@link #getCache(String)}相反, 此方法不会通过 {@link #getMissingCache(String)}触发缺少的缓存的延迟创建.
	 * 
	 * @param name 缓存标识符 (must not be {@code null})
	 * 
	 * @return 关联的Cache实例, 或{@code null}
	 */
	protected final Cache lookupCache(String name) {
		return this.cacheMap.get(name);
	}

	/**
	 * 使用此管理器动态注册其他缓存.
	 * 
	 * @param cache 要注册的缓存
	 * 
	 * @deprecated as of Spring 4.3, in favor of {@link #getMissingCache(String)}
	 */
	@Deprecated
	protected final void addCache(Cache cache) {
		String name = cache.getName();
		synchronized (this.cacheMap) {
			if (this.cacheMap.put(name, decorateCache(cache)) == null) {
				updateCacheNames(name);
			}
		}
	}

	/**
	 * 使用给定名称更新公开的{@link #cacheNames}集.
	 * <p>这将始终在完整的{@link #cacheMap}锁中调用, 并且有效地表现为保留顺序的{@code CopyOnWriteArraySet}, 但是作为不可修改的引用公开.
	 * 
	 * @param name 要添加的缓存的名称
	 */
	private void updateCacheNames(String name) {
		Set<String> cacheNames = new LinkedHashSet<String>(this.cacheNames.size() + 1);
		cacheNames.addAll(this.cacheNames);
		cacheNames.add(name);
		this.cacheNames = Collections.unmodifiableSet(cacheNames);
	}


	// Overridable template methods for cache initialization

	/**
	 * 装饰给定的Cache对象.
	 * 
	 * @param cache 要添加到此CacheManager的Cache对象
	 * 
	 * @return 要使用的装饰的Cache对象, 或者默认情况下只是传入的Cache对象
	 */
	protected Cache decorateCache(Cache cache) {
		return cache;
	}

	/**
	 * 如果此类缓存不存在或无法即时创建, 则使用指定的{@code name}或{@code null}返回缺少的缓存.
	 * <p>如果本机提供程序支持, 则可以在运行时创建一些缓存.
	 * 如果按名称查找不会产生结果, 则子类有机会在运行时注册此类缓存.
	 * 返回的缓存将自动添加到此实例.
	 * 
	 * @param name 要检索的缓存的名称
	 * 
	 * @return 缺少的缓存, 或{@code null}如果不存在或可以创建这样的缓存
	 */
	protected Cache getMissingCache(String name) {
		return null;
	}

}
