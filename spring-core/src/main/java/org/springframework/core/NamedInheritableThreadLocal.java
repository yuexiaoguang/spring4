package org.springframework.core;

import org.springframework.util.Assert;

/**
 * {@link InheritableThreadLocal}子类, 它将指定的名称公开为{@link #toString()}结果 (允许内省).
 */
public class NamedInheritableThreadLocal<T> extends InheritableThreadLocal<T> {

	private final String name;


	/**
	 * @param name 此ThreadLocal的描述性名称
	 */
	public NamedInheritableThreadLocal(String name) {
		Assert.hasText(name, "Name must not be empty");
		this.name = name;
	}

	@Override
	public String toString() {
		return this.name;
	}

}
