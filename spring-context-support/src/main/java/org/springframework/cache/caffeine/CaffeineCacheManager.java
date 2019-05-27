package org.springframework.cache.caffeine;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.CaffeineSpec;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * {@link CacheManager}实现, 为每个{@link #getCache}请求延迟构建{@link CaffeineCache}实例.
 * 还支持'static'模式, 其中通过{@link #setCacheNames}预定义了一组缓存名称, 在运行时没有动态创建其他缓存区域.
 *
 * <p>底层缓存的配置可以通过{@link Caffeine}构建器或{@link CaffeineSpec}进行微调,
 * 通过{@link #setCaffeine}/{@link #setCaffeineSpec}传递到此CacheManager中.
 * 符合{@link CaffeineSpec}的表达式值也可以通过{@link #setCacheSpecification "cacheSpecification"} bean属性应用.
 *
 * <p>Requires Caffeine 2.1 or higher.
 */
public class CaffeineCacheManager implements CacheManager {

	private final ConcurrentMap<String, Cache> cacheMap = new ConcurrentHashMap<String, Cache>(16);

	private boolean dynamic = true;

	private Caffeine<Object, Object> cacheBuilder = Caffeine.newBuilder();

	private CacheLoader<Object, Object> cacheLoader;

	private boolean allowNullValues = true;


	/**
	 * 构造一个动态的CaffeineCacheManager, 在请求时延迟创建缓存实例.
	 */
	public CaffeineCacheManager() {
	}

	/**
	 * 构造一个静态CaffeineCacheManager, 仅管理指定缓存名称的缓存.
	 */
	public CaffeineCacheManager(String... cacheNames) {
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
				this.cacheMap.put(name, createCaffeineCache(name));
			}
			this.dynamic = false;
		}
		else {
			this.dynamic = true;
		}
	}

	/**
	 * 设置Caffeine用于构建每个{@link CaffeineCache}实例.
	 */
	public void setCaffeine(Caffeine<Object, Object> caffeine) {
		Assert.notNull(caffeine, "Caffeine must not be null");
		doSetCaffeine(caffeine);
	}

	/**
	 * 设置{@link CaffeineSpec}以用于构建每个{@link CaffeineCache}实例.
	 */
	public void setCaffeineSpec(CaffeineSpec caffeineSpec) {
		doSetCaffeine(Caffeine.from(caffeineSpec));
	}

	/**
	 * 设置Caffeine缓存规范字符串, 用于构建每个{@link CaffeineCache}实例.
	 * 给定的值需要符合Caffeine的{@link CaffeineSpec} (see its javadoc).
	 */
	public void setCacheSpecification(String cacheSpecification) {
		doSetCaffeine(Caffeine.from(cacheSpecification));
	}

	/**
	 * 设置Caffeine CacheLoader用于构建每个{@link CaffeineCache}实例, 将其转换为LoadingCache.
	 */
	public void setCacheLoader(CacheLoader<Object, Object> cacheLoader) {
		if (!ObjectUtils.nullSafeEquals(this.cacheLoader, cacheLoader)) {
			this.cacheLoader = cacheLoader;
			refreshKnownCaches();
		}
	}

	/**
	 * 指定是否接受并转换此缓存管理器中所有缓存的{@code null}值.
	 * <p>默认"true", 尽管Caffeine本身不支持{@code null}值.
	 * 内部持有者对象将用于存储用户级{@code null}.
	 */
	public void setAllowNullValues(boolean allowNullValues) {
		if (this.allowNullValues != allowNullValues) {
			this.allowNullValues = allowNullValues;
			refreshKnownCaches();
		}
	}

	/**
	 * 返回此缓存管理器是否接受并转换其缓存的所有{@code null}值.
	 */
	public boolean isAllowNullValues() {
		return this.allowNullValues;
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
					cache = createCaffeineCache(name);
					this.cacheMap.put(name, cache);
				}
			}
		}
		return cache;
	}

	/**
	 * 为指定的缓存名称创建新的CaffeineCache实例.
	 * 
	 * @param name 缓存名称
	 * 
	 * @return Spring CaffeineCache适配器 (或其装饰器)
	 */
	protected Cache createCaffeineCache(String name) {
		return new CaffeineCache(name, createNativeCaffeineCache(name), isAllowNullValues());
	}

	/**
	 * 为指定的缓存名称创建本机Caffeine Cache实例.
	 * 
	 * @param name 缓存名称
	 * 
	 * @return 本机Caffeine Cache实例
	 */
	protected com.github.benmanes.caffeine.cache.Cache<Object, Object> createNativeCaffeineCache(String name) {
		if (this.cacheLoader != null) {
			return this.cacheBuilder.build(this.cacheLoader);
		}
		else {
			return this.cacheBuilder.build();
		}
	}

	private void doSetCaffeine(Caffeine<Object, Object> cacheBuilder) {
		if (!ObjectUtils.nullSafeEquals(this.cacheBuilder, cacheBuilder)) {
			this.cacheBuilder = cacheBuilder;
			refreshKnownCaches();
		}
	}

	/**
	 * 使用此管理器的当前状态再次创建已知缓存.
	 */
	private void refreshKnownCaches() {
		for (Map.Entry<String, Cache> entry : this.cacheMap.entrySet()) {
			entry.setValue(createCaffeineCache(entry.getKey()));
		}
	}
}
