package org.springframework.cache.support;

import org.springframework.cache.Cache;

/**
 * {@link Cache}实现的公共基类, 在将它们传递给底层存储之前需要调整{@code null}值 (以及可能的其他此类特殊值).
 *
 * <p>如果配置为支持{@code null}值, 则使用内部 {@link NullValue#INSTANCE}透明地替换给定的{@code null}用户值, 由 {@link #isAllowNullValues()}指示.
 */
public abstract class AbstractValueAdaptingCache implements Cache {

	private final boolean allowNullValues;


	/**
	 * @param allowNullValues 是否允许{@code null}值
	 */
	protected AbstractValueAdaptingCache(boolean allowNullValues) {
		this.allowNullValues = allowNullValues;
	}


	/**
	 * 返回此缓存中是否允许{@code null}值.
	 */
	public final boolean isAllowNullValues() {
		return this.allowNullValues;
	}

	@Override
	public ValueWrapper get(Object key) {
		Object value = lookup(key);
		return toValueWrapper(value);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T get(Object key, Class<T> type) {
		Object value = fromStoreValue(lookup(key));
		if (value != null && type != null && !type.isInstance(value)) {
			throw new IllegalStateException(
					"Cached value is not of required type [" + type.getName() + "]: " + value);
		}
		return (T) value;
	}

	/**
	 * 在底层存储中执行实际查找.
	 * 
	 * @param key 要返回其关联值的键
	 * 
	 * @return Key的原始存储值, 或{@code null}
	 */
	protected abstract Object lookup(Object key);


	/**
	 * 将内部存储中的给定值转换为get方法返回的用户值 (适应 {@code null}).
	 * 
	 * @param storeValue 保存的值
	 * 
	 * @return 返回给用户的值
	 */
	protected Object fromStoreValue(Object storeValue) {
		if (this.allowNullValues && storeValue == NullValue.INSTANCE) {
			return null;
		}
		return storeValue;
	}

	/**
	 * 将传递给put方法的给定用户的值转换为内部存储中的值 (adapting {@code null}).
	 * 
	 * @param userValue 给定用户的值
	 * 
	 * @return 保存的值
	 */
	protected Object toStoreValue(Object userValue) {
		if (this.allowNullValues && userValue == null) {
			return NullValue.INSTANCE;
		}
		return userValue;
	}

	/**
	 * 使用{@link SimpleValueWrapper}包装给定存储的值, 同时进行{@link #fromStoreValue}转换.
	 * 对{@link #get(Object)}和{@link #putIfAbsent(Object, Object)}实现很有用.
	 * 
	 * @param storeValue 原始值
	 * 
	 * @return 包装后的值
	 */
	protected Cache.ValueWrapper toValueWrapper(Object storeValue) {
		return (storeValue != null ? new SimpleValueWrapper(fromStoreValue(storeValue)) : null);
	}
}
