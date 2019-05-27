package org.springframework.cache.jcache;

import java.util.concurrent.Callable;
import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.MutableEntry;

import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.util.Assert;

/**
 * 在{@link javax.cache.Cache}实例之上, {@link org.springframework.cache.Cache}实现.
 *
 * <p>Note: 从Spring 4.0开始, 此类已针对JCache 1.0进行了更新.
 */
public class JCacheCache extends AbstractValueAdaptingCache {

	private final javax.cache.Cache<Object, Object> cache;


	/**
	 * @param jcache 支持的JCache Cache实例
	 */
	public JCacheCache(javax.cache.Cache<Object, Object> jcache) {
		this(jcache, true);
	}

	/**
	 * @param jcache 支持的JCache Cache实例
	 * @param allowNullValues 是否接受并转换此缓存的null值
	 */
	public JCacheCache(javax.cache.Cache<Object, Object> jcache, boolean allowNullValues) {
		super(allowNullValues);
		Assert.notNull(jcache, "Cache must not be null");
		this.cache = jcache;
	}


	@Override
	public final String getName() {
		return this.cache.getName();
	}

	@Override
	public final javax.cache.Cache<Object, Object> getNativeCache() {
		return this.cache;
	}

	@Override
	protected Object lookup(Object key) {
		return this.cache.get(key);
	}

	@Override
	public <T> T get(Object key, Callable<T> valueLoader) {
		try {
			return this.cache.invoke(key, new ValueLoaderEntryProcessor<T>(), valueLoader);
		}
		catch (EntryProcessorException ex) {
			throw new ValueRetrievalException(key, valueLoader, ex.getCause());
		}
	}

	@Override
	public void put(Object key, Object value) {
		this.cache.put(key, toStoreValue(value));
	}

	@Override
	public ValueWrapper putIfAbsent(Object key, Object value) {
		boolean set = this.cache.putIfAbsent(key, toStoreValue(value));
		return (set ? null : get(key));
	}

	@Override
	public void evict(Object key) {
		this.cache.remove(key);
	}

	@Override
	public void clear() {
		this.cache.removeAll();
	}


	private class ValueLoaderEntryProcessor<T> implements EntryProcessor<Object, Object, T> {

		@SuppressWarnings("unchecked")
		@Override
		public T process(MutableEntry<Object, Object> entry, Object... arguments) throws EntryProcessorException {
			Callable<T> valueLoader = (Callable<T>) arguments[0];
			if (entry.exists()) {
				return (T) fromStoreValue(entry.getValue());
			}
			else {
				T value;
				try {
					value = valueLoader.call();
				}
				catch (Exception ex) {
					throw new EntryProcessorException("Value loader '" + valueLoader + "' failed " +
							"to compute  value for key '" + entry.getKey() + "'", ex);
				}
				entry.setValue(toStoreValue(value));
				return value;
			}
		}
	}

}
