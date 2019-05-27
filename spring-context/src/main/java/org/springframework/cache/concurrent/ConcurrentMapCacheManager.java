package org.springframework.cache.concurrent;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.core.serializer.support.SerializationDelegate;

/**
 * {@link CacheManager}实现, 为每个{@link #getCache}请求延迟构建 {@link ConcurrentMapCache}实例.
 * 还支持 'static'模式, 其中通过 {@link #setCacheNames}预定义了一组缓存名称, 在运行时没有动态创建其他缓存区域.
 *
 * <p>Note: 这绝不是复杂的CacheManager; 它没有缓存配置选项.
 * 但是, 它可能对测试或简单缓存场景很有用. 对于高级本地缓存需求, 考虑
 * {@link org.springframework.cache.jcache.JCacheCacheManager},
 * {@link org.springframework.cache.ehcache.EhCacheCacheManager},
 * {@link org.springframework.cache.caffeine.CaffeineCacheManager},
 * {@link org.springframework.cache.guava.GuavaCacheManager}.
 */
public class ConcurrentMapCacheManager implements CacheManager, BeanClassLoaderAware {

	private final ConcurrentMap<String, Cache> cacheMap = new ConcurrentHashMap<String, Cache>(16);

	private boolean dynamic = true;

	private boolean allowNullValues = true;

	private boolean storeByValue = false;

	private SerializationDelegate serialization;


	/**
	 * 构造一个动态的 ConcurrentMapCacheManager, 在请求时延迟创建缓存实例.
	 */
	public ConcurrentMapCacheManager() {
	}

	/**
	 * 构造一个静态的 ConcurrentMapCacheManager, 仅管理指定缓存名称的缓存.
	 */
	public ConcurrentMapCacheManager(String... cacheNames) {
		setCacheNames(Arrays.asList(cacheNames));
	}


	/**
	 * 为此CacheManager的'static'模式指定一组缓存名称.
	 * <p>调用此方法后, 将固定缓存及其名称的数量, 而不会在运行时创建其他缓存区域.
	 * <p>使用{@code null}集合参数调用此方法会将模式重置为'dynamic', 从而允许再次创建缓存.
	 */
	public void setCacheNames(Collection<String> cacheNames) {
		if (cacheNames != null) {
			for (String name : cacheNames) {
				this.cacheMap.put(name, createConcurrentMapCache(name));
			}
			this.dynamic = false;
		}
		else {
			this.dynamic = true;
		}
	}

	/**
	 * 指定是否接受并转换此缓存管理器中所有缓存的{@code null}值.
	 * <p>默认是 "true", 尽管ConcurrentHashMap本身不支持{@code null}值.
	 * 内部持有者对象将用于存储用户级{@code null}.
	 * <p>Note: 更改null值设置将重置所有现有高速缓存, 以使用新的null值要求重新配置它们.
	 */
	public void setAllowNullValues(boolean allowNullValues) {
		if (allowNullValues != this.allowNullValues) {
			this.allowNullValues = allowNullValues;
			// 需要使用新的null值配置重新创建所有Cache实例...
			recreateCaches();
		}
	}

	/**
	 * 返回此缓存管理器是否接受并转换其所有缓存的{@code null}值.
	 */
	public boolean isAllowNullValues() {
		return this.allowNullValues;
	}

	/**
	 * 指定此高速缓存管理器是否为其所有缓存存储每个条目的副本 ({@code true} 或引用 ({@code false}.
	 * <p>默认是 "false" 这样就可以存储值本身, 并且缓存值不需要可序列化.
	 * <p>Note: store-by-value设置的更改将重置所有现有缓存, 以使用新的store-by-value要求重新配置它们.
	 */
	public void setStoreByValue(boolean storeByValue) {
		if (storeByValue != this.storeByValue) {
			this.storeByValue = storeByValue;
			// Need to recreate all Cache instances with the new store-by-value configuration...
			recreateCaches();
		}
	}

	/**
	 * 返回此缓存管理器是否存储每个条目的副本或其所有缓存的引用.
	 * 如果启用了按值存储, 则任何缓存条目都必须是可序列化的.
	 */
	public boolean isStoreByValue() {
		return this.storeByValue;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.serialization = new SerializationDelegate(classLoader);
		// Need to recreate all Cache instances with new ClassLoader in store-by-value mode...
		if (isStoreByValue()) {
			recreateCaches();
		}
	}


	@Override
	public Collection<String> getCacheNames() {
		return Collections.unmodifiableSet(this.cacheMap.keySet());
	}

	@Override
	public Cache getCache(String name) {
		Cache cache = this.cacheMap.get(name);
		if (cache == null && this.dynamic) {
			synchronized (this.cacheMap) {
				cache = this.cacheMap.get(name);
				if (cache == null) {
					cache = createConcurrentMapCache(name);
					this.cacheMap.put(name, cache);
				}
			}
		}
		return cache;
	}

	private void recreateCaches() {
		for (Map.Entry<String, Cache> entry : this.cacheMap.entrySet()) {
			entry.setValue(createConcurrentMapCache(entry.getKey()));
		}
	}

	/**
	 * 为指定的缓存名称创建新的ConcurrentMapCache实例.
	 * 
	 * @param name 缓存名称
	 * 
	 * @return ConcurrentMapCache (或其装饰者)
	 */
	protected Cache createConcurrentMapCache(String name) {
		SerializationDelegate actualSerialization = (isStoreByValue() ? this.serialization : null);
		return new ConcurrentMapCache(name, new ConcurrentHashMap<Object, Object>(256),
				isAllowNullValues(), actualSerialization);

	}

}
