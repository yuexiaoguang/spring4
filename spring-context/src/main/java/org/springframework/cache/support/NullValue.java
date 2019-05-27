package org.springframework.cache.support;

import java.io.Serializable;

/**
 * 简单的可序列化类, 用作缓存存储的{@code null}替换, 否则不支持{@code null}值.
 */
public final class NullValue implements Serializable {

	/**
	 * {@code null}替换的规范表示,
	 * 由{@link AbstractValueAdaptingCache#toStoreValue}/ {@link AbstractValueAdaptingCache#fromStoreValue}的默认实现使用.
	 */
	public static final Object INSTANCE = new NullValue();

	private static final long serialVersionUID = 1L;


	private NullValue() {
	}

	private Object readResolve() {
		return INSTANCE;
	}

}
