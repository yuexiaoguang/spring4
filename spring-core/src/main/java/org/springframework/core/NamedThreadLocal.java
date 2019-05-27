package org.springframework.core;

import org.springframework.util.Assert;

/**
 * {@link ThreadLocal}子类, 它将指定的名称公开为{@link #toString()}结果 (允许内省).
 */
public class NamedThreadLocal<T> extends ThreadLocal<T> {

	private final String name;


	/**
	 * @param name 此ThreadLocal的描述性名称
	 */
	public NamedThreadLocal(String name) {
		Assert.hasText(name, "Name must not be empty");
		this.name = name;
	}

	@Override
	public String toString() {
		return this.name;
	}

}
