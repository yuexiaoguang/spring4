package org.springframework.cache.support;

import org.springframework.cache.Cache.ValueWrapper;

/**
 * {@link org.springframework.cache.Cache.ValueWrapper}的直接实现,
 * 简单保存构造中给出的值并从 {@link #get()}返回它.
 */
public class SimpleValueWrapper implements ValueWrapper {

	private final Object value;


	/**
	 * @param value 要暴露的值 (may be {@code null})
	 */
	public SimpleValueWrapper(Object value) {
		this.value = value;
	}


	/**
	 * 返回构造时给定的值.
	 */
	@Override
	public Object get() {
		return this.value;
	}

}
