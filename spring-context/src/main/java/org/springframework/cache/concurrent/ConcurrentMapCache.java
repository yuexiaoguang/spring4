package org.springframework.cache.concurrent;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.core.serializer.support.SerializationDelegate;
import org.springframework.util.Assert;

/**
 * 基于核心JDK {@code java.util.concurrent}包的简单{@link org.springframework.cache.Cache}实现.
 *
 * <p>用于测试或简单缓存场景, 通常与{@link org.springframework.cache.support.SimpleCacheManager}结合使用,
 * 或通过 {@link ConcurrentMapCacheManager}动态实现.
 *
 * <p><b>Note:</b> 由于 {@link ConcurrentHashMap} (使用的默认实现) 不允许存储{@code null}值,
 * 此类将使用预定义的内部对象替换它们.
 * 可以通过 {@link #ConcurrentMapCache(String, ConcurrentMap, boolean)}构造函数更改此行为.
 */
public class ConcurrentMapCache extends AbstractValueAdaptingCache {

	private final String name;

	private final ConcurrentMap<Object, Object> store;

	private final SerializationDelegate serialization;


	/**
	 * @param name 缓存的名称
	 */
	public ConcurrentMapCache(String name) {
		this(name, new ConcurrentHashMap<Object, Object>(256), true);
	}

	/**
	 * @param name 缓存的名称
	 * @param allowNullValues 是否接受并转换此缓存的{@code null}值
	 */
	public ConcurrentMapCache(String name, boolean allowNullValues) {
		this(name, new ConcurrentHashMap<Object, Object>(256), allowNullValues);
	}

	/**
	 * @param name 缓存的名称
	 * @param store 用作内部存储的ConcurrentMap
	 * @param allowNullValues 是否接受并转换此缓存的{@code null}值
	 */
	public ConcurrentMapCache(String name, ConcurrentMap<Object, Object> store, boolean allowNullValues) {
		this(name, store, allowNullValues, null);
	}

	/**
	 * 如果指定了{@link SerializationDelegate}, 则启用{@link #isStoreByValue() store-by-value}
	 * 
	 * @param name 缓存的名称
	 * @param store 用作内部存储的ConcurrentMap
	 * @param allowNullValues 是否允许{@code null}值 (使它们适应内部的null holder值)
	 * @param serialization 用于序列化缓存条目的{@link SerializationDelegate}, 或用于存储引用的{@code null}
	 */
	protected ConcurrentMapCache(String name, ConcurrentMap<Object, Object> store,
			boolean allowNullValues, SerializationDelegate serialization) {

		super(allowNullValues);
		Assert.notNull(name, "Name must not be null");
		Assert.notNull(store, "Store must not be null");
		this.name = name;
		this.store = store;
		this.serialization = serialization;
	}


	/**
	 * 返回此缓存是否存储每个条目的副本 ({@code true}) 或引用 ({@code false}, 默认).
	 * 如果启用了按值存储, 则缓存中的每个条目都必须是可序列化的.
	 */
	public final boolean isStoreByValue() {
		return (this.serialization != null);
	}

	@Override
	public final String getName() {
		return this.name;
	}

	@Override
	public final ConcurrentMap<Object, Object> getNativeCache() {
		return this.store;
	}

	@Override
	protected Object lookup(Object key) {
		return this.store.get(key);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T get(Object key, Callable<T> valueLoader) {
		// 首先尝试在ConcurrentHashMap上进行高效查找...
		ValueWrapper storeValue = get(key);
		if (storeValue != null) {
			return (T) storeValue.get();
		}

		// No value found -> load value within full synchronization.
		synchronized (this.store) {
			storeValue = get(key);
			if (storeValue != null) {
				return (T) storeValue.get();
			}

			T value;
			try {
				value = valueLoader.call();
			}
			catch (Throwable ex) {
				throw new ValueRetrievalException(key, valueLoader, ex);
			}
			put(key, value);
			return value;
		}
	}

	@Override
	public void put(Object key, Object value) {
		this.store.put(key, toStoreValue(value));
	}

	@Override
	public ValueWrapper putIfAbsent(Object key, Object value) {
		Object existing = this.store.putIfAbsent(key, toStoreValue(value));
		return toValueWrapper(existing);
	}

	@Override
	public void evict(Object key) {
		this.store.remove(key);
	}

	@Override
	public void clear() {
		this.store.clear();
	}

	@Override
	protected Object toStoreValue(Object userValue) {
		Object storeValue = super.toStoreValue(userValue);
		if (this.serialization != null) {
			try {
				return serializeValue(storeValue);
			}
			catch (Throwable ex) {
				throw new IllegalArgumentException("Failed to serialize cache value '" + userValue +
						"'. Does it implement Serializable?", ex);
			}
		}
		else {
			return storeValue;
		}
	}

	private Object serializeValue(Object storeValue) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			this.serialization.serialize(storeValue, out);
			return out.toByteArray();
		}
		finally {
			out.close();
		}
	}

	@Override
	protected Object fromStoreValue(Object storeValue) {
		if (this.serialization != null) {
			try {
				return super.fromStoreValue(deserializeValue(storeValue));
			}
			catch (Throwable ex) {
				throw new IllegalArgumentException("Failed to deserialize cache value '" + storeValue + "'", ex);
			}
		}
		else {
			return super.fromStoreValue(storeValue);
		}

	}

	private Object deserializeValue(Object storeValue) throws IOException {
		ByteArrayInputStream in = new ByteArrayInputStream((byte[]) storeValue);
		try {
			return this.serialization.deserialize(in);
		}
		finally {
			in.close();
		}
	}

}
