package org.springframework.core;

import java.lang.reflect.Method;

import org.springframework.util.ObjectUtils;

/**
 * 针对特定目标类的方法的公共键类, 包括{@link #toString()}表示和{@link Comparable}支持
 * (如Java 8中自定义{{@code HashMap}键的建议).
 */
public final class MethodClassKey implements Comparable<MethodClassKey> {

	private final Method method;

	private final Class<?> targetClass;


	/**
	 * 为给定方法和目标类创建Key对象.
	 * 
	 * @param method 要包装的方法 (must not be {@code null})
	 * @param targetClass 将调用该方法的目标类(如果与声明类相同, 则可以是{@code null})
	 */
	public MethodClassKey(Method method, Class<?> targetClass) {
		this.method = method;
		this.targetClass = targetClass;
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof MethodClassKey)) {
			return false;
		}
		MethodClassKey otherKey = (MethodClassKey) other;
		return (this.method.equals(otherKey.method) &&
				ObjectUtils.nullSafeEquals(this.targetClass, otherKey.targetClass));
	}

	@Override
	public int hashCode() {
		return this.method.hashCode() + (this.targetClass != null ? this.targetClass.hashCode() * 29 : 0);
	}

	@Override
	public String toString() {
		return this.method + (this.targetClass != null ? " on " + this.targetClass : "");
	}

	@Override
	public int compareTo(MethodClassKey other) {
		int result = this.method.getName().compareTo(other.method.getName());
		if (result == 0) {
			result = this.method.toString().compareTo(other.method.toString());
			if (result == 0 && this.targetClass != null && other.targetClass != null) {
				result = this.targetClass.getName().compareTo(other.targetClass.getName());
			}
		}
		return result;
	}

}
