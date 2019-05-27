package org.springframework.cache.support;

import java.util.concurrent.Callable;

import org.springframework.cache.Cache;
import org.springframework.util.Assert;

/**
 * 适用于禁用缓存的无操作{@link Cache}实现.
 *
 * <p>将简单地接受进入缓存的所有条目, 而不是实际存储它们.
 */
public class NoOpCache implements Cache {

	private final String name;


	/**
	 * @param name 缓存的名称
	 */
	public NoOpCache(String name) {
		Assert.notNull(name, "Cache name must not be null");
		this.name = name;
	}


	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public Object getNativeCache() {
		return null;
	}

	@Override
	public ValueWrapper get(Object key) {
		return null;
	}

	@Override
	public <T> T get(Object key, Class<T> type) {
		return null;
	}

	@Override
	public <T> T get(Object key, Callable<T> valueLoader) {
		try {
			return valueLoader.call();
		}
		catch (Exception ex) {
			throw new ValueRetrievalException(key, valueLoader, ex);
		}
	}

	@Override
	public void put(Object key, Object value) {
	}

	@Override
	public ValueWrapper putIfAbsent(Object key, Object value) {
		return null;
	}

	@Override
	public void evict(Object key) {
	}

	@Override
	public void clear() {
	}

}
