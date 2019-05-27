package org.springframework.cache.guava;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheBuilderSpec;
import com.google.common.cache.CacheLoader;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * {@link CacheManager}实现, 为每个{@link #getCache}请求延迟构建{@link GuavaCache}实例.
 * 还支持'static'模式, 其中通过{@link #setCacheNames}预定义了一组缓存名称, 在运行时不会动态创建其他缓存区域.
 *
 * <p>底层缓存的配置可以通过Guava {@link CacheBuilder}或{@link CacheBuilderSpec}进行微调,
 * 通过{@link #setCacheBuilder}/{@link #setCacheBuilderSpec}传递到此CacheManager.
 * 符合{@link CacheBuilderSpec}的表达式值也可以通过{@link #setCacheSpecification "cacheSpecification"} bean属性应用.
 *
 * <p>Requires Google Guava 12.0 or higher.
 */
public class GuavaCacheManager implements CacheManager {

	private final ConcurrentMap<String, Cache> cacheMap = new ConcurrentHashMap<String, Cache>(16);

	private boolean dynamic = true;

	private CacheBuilder<Object, Object> cacheBuilder = CacheBuilder.newBuilder();

	private CacheLoader<Object, Object> cacheLoader;

	private boolean allowNullValues = true;


	/**
	 * 构建一个动态的GuavaCacheManager, 在请求时延迟创建缓存实例.
	 */
	public GuavaCacheManager() {
	}

	/**
	 * 构造一个静态GuavaCacheManager, 仅管理指定缓存名称的缓存.
	 */
	public GuavaCacheManager(String... cacheNames) {
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
				this.cacheMap.put(name, createGuavaCache(name));
			}
			this.dynamic = false;
		}
		else {
			this.dynamic = true;
		}
	}

	/**
	 * 设置用于构建每个单独的{@link GuavaCache}实例的Guava CacheBuilder.
	 */
	public void setCacheBuilder(CacheBuilder<Object, Object> cacheBuilder) {
		Assert.notNull(cacheBuilder, "CacheBuilder must not be null");
		doSetCacheBuilder(cacheBuilder);
	}

	/**
	 * 设置用于构建每个单独的{@link GuavaCache}实例的Guava CacheBuilderSpec.
	 */
	public void setCacheBuilderSpec(CacheBuilderSpec cacheBuilderSpec) {
		doSetCacheBuilder(CacheBuilder.from(cacheBuilderSpec));
	}

	/**
	 * 设置Guava缓存规范String, 以用于构建每个单独的{@link GuavaCache}实例.
	 * 给定的值需要符合Guava的{@link CacheBuilderSpec} (see its javadoc).
	 */
	public void setCacheSpecification(String cacheSpecification) {
		doSetCacheBuilder(CacheBuilder.from(cacheSpecification));
	}

	/**
	 * 设置Guava CacheLoader以用于构建每个单独的{@link GuavaCache}实例, 将其转换为LoadingCache.
	 */
	public void setCacheLoader(CacheLoader<Object, Object> cacheLoader) {
		if (!ObjectUtils.nullSafeEquals(this.cacheLoader, cacheLoader)) {
			this.cacheLoader = cacheLoader;
			refreshKnownCaches();
		}
	}

	/**
	 * 指定是否接受并转换此缓存管理器中所有缓存的{@code null}值.
	 * <p>默认 "true", 尽管Guava本身不支持{@code null}值.
	 * 内部持有者对象将用于存储用户级{@code null}.
	 */
	public void setAllowNullValues(boolean allowNullValues) {
		if (this.allowNullValues != allowNullValues) {
			this.allowNullValues = allowNullValues;
			refreshKnownCaches();
		}
	}

	/**
	 * 返回此缓存管理器是否接受并转换其所有缓存的{@code null}值.
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
					cache = createGuavaCache(name);
					this.cacheMap.put(name, cache);
				}
			}
		}
		return cache;
	}

	/**
	 * 为指定的缓存名称创建新的GuavaCache实例.
	 * 
	 * @param name 缓存名称
	 * 
	 * @return Spring GuavaCache适配器 (或其装饰器)
	 */
	protected Cache createGuavaCache(String name) {
		return new GuavaCache(name, createNativeGuavaCache(name), isAllowNullValues());
	}

	/**
	 * 为指定的缓存名称创建本机Guava缓存实例.
	 * 
	 * @param name 缓存名称
	 * 
	 * @return 本机Guava缓存实例
	 */
	protected com.google.common.cache.Cache<Object, Object> createNativeGuavaCache(String name) {
		if (this.cacheLoader != null) {
			return this.cacheBuilder.build(this.cacheLoader);
		}
		else {
			return this.cacheBuilder.build();
		}
	}

	private void doSetCacheBuilder(CacheBuilder<Object, Object> cacheBuilder) {
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
			entry.setValue(createGuavaCache(entry.getKey()));
		}
	}
}
