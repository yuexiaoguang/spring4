package org.springframework.cache.interceptor;

import java.io.Serializable;
import java.util.Arrays;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 从{@link SimpleKeyGenerator}返回的简单Key.
 */
@SuppressWarnings("serial")
public class SimpleKey implements Serializable {

	public static final SimpleKey EMPTY = new SimpleKey();

	private final Object[] params;
	private final int hashCode;


	/**
	 * @param elements Key的元素
	 */
	public SimpleKey(Object... elements) {
		Assert.notNull(elements, "Elements must not be null");
		this.params = new Object[elements.length];
		System.arraycopy(elements, 0, this.params, 0, elements.length);
		this.hashCode = Arrays.deepHashCode(this.params);
	}

	@Override
	public boolean equals(Object obj) {
		return (this == obj || (obj instanceof SimpleKey
				&& Arrays.deepEquals(this.params, ((SimpleKey) obj).params)));
	}

	@Override
	public final int hashCode() {
		return this.hashCode;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " [" + StringUtils.arrayToCommaDelimitedString(this.params) + "]";
	}

}
