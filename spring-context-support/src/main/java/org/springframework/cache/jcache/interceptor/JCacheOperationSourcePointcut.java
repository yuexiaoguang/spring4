package org.springframework.cache.jcache.interceptor;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.util.ObjectUtils;

/**
 * 如果底层{@link JCacheOperationSource}具有给定方法的操作, 则匹配的切点.
 */
@SuppressWarnings("serial")
public abstract class JCacheOperationSourcePointcut
		extends StaticMethodMatcherPointcut implements Serializable {

	@Override
	public boolean matches(Method method, Class<?> targetClass) {
		JCacheOperationSource cas = getCacheOperationSource();
		return (cas != null && cas.getCacheOperation(method, targetClass) != null);
	}

	/**
	 * 获取底层{@link JCacheOperationSource} (may be {@code null}).
	 * 由子类实现.
	 */
	protected abstract JCacheOperationSource getCacheOperationSource();

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof JCacheOperationSourcePointcut)) {
			return false;
		}
		JCacheOperationSourcePointcut otherPc = (JCacheOperationSourcePointcut) other;
		return ObjectUtils.nullSafeEquals(getCacheOperationSource(), otherPc.getCacheOperationSource());
	}

	@Override
	public int hashCode() {
		return JCacheOperationSourcePointcut.class.hashCode();
	}

	@Override
	public String toString() {
		return getClass().getName() + ": " + getCacheOperationSource();
	}

}
