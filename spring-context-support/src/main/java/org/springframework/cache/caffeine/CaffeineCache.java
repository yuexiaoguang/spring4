package org.springframework.cache.caffeine;

import java.util.concurrent.Callable;
import java.util.function.Function;

import com.github.benmanes.caffeine.cache.LoadingCache;

import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.lang.UsesJava8;
import org.springframework.util.Assert;

/**
 * Spring {@link org.springframework.cache.Cache}适配器实现,
 * 在Caffeine {@link com.github.benmanes.caffeine.cache.Cache}实例之上.
 *
 * <p>需要 Caffeine 2.1或更高.
 */
@UsesJava8
public class CaffeineCache extends AbstractValueAdaptingCache {

	private final String name;

	private final com.github.benmanes.caffeine.cache.Cache<Object, Object> cache;


	/**
	 * @param name 缓存的名称
	 * @param cache 支持的Caffeine Cache实例
	 */
	public CaffeineCache(String name, com.github.benmanes.caffeine.cache.Cache<Object, Object> cache) {
		this(name, cache, true);
	}

	/**
	 * @param name 缓存的名称
	 * @param cache 支持的Caffeine Cache实例
	 * @param allowNullValues 是否接受并转换此缓存的{@code null}值
	 */
	public CaffeineCache(String name, com.github.benmanes.caffeine.cache.Cache<Object, Object> cache,
			boolean allowNullValues) {

		super(allowNullValues);
		Assert.notNull(name, "Name must not be null");
		Assert.notNull(cache, "Cache must not be null");
		this.name = name;
		this.cache = cache;
	}


	@Override
	public final String getName() {
		return this.name;
	}

	@Override
	public final com.github.benmanes.caffeine.cache.Cache<Object, Object> getNativeCache() {
		return this.cache;
	}

	@Override
	public ValueWrapper get(Object key) {
		if (this.cache instanceof LoadingCache) {
			Object value = ((LoadingCache<Object, Object>) this.cache).get(key);
			return toValueWrapper(value);
		}
		return super.get(key);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T get(Object key, final Callable<T> valueLoader) {
		return (T) fromStoreValue(this.cache.get(key, new LoadFunction(valueLoader)));
	}

	@Override
	protected Object lookup(Object key) {
		return this.cache.getIfPresent(key);
	}

	@Override
	public void put(Object key, Object value) {
		this.cache.put(key, toStoreValue(value));
	}

	@Override
	public ValueWrapper putIfAbsent(Object key, final Object value) {
		PutIfAbsentFunction callable = new PutIfAbsentFunction(value);
		Object result = this.cache.get(key, callable);
		return (callable.called ? null : toValueWrapper(result));
	}

	@Override
	public void evict(Object key) {
		this.cache.invalidate(key);
	}

	@Override
	public void clear() {
		this.cache.invalidateAll();
	}


	private class PutIfAbsentFunction implements Function<Object, Object> {

		private final Object value;

		private boolean called;

		public PutIfAbsentFunction(Object value) {
			this.value = value;
		}

		@Override
		public Object apply(Object key) {
			this.called = true;
			return toStoreValue(this.value);
		}
	}


	private class LoadFunction implements Function<Object, Object> {

		private final Callable<?> valueLoader;

		public LoadFunction(Callable<?> valueLoader) {
			this.valueLoader = valueLoader;
		}

		@Override
		public Object apply(Object o) {
			try {
				return toStoreValue(valueLoader.call());
			}
			catch (Exception ex) {
				throw new ValueRetrievalException(o, valueLoader, ex);
			}
		}
	}

}
