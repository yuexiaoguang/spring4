package org.springframework.cache.jcache.interceptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.cache.annotation.CacheMethodDetails;

import static java.util.Arrays.*;

/**
 * 默认的{@link CacheMethodDetails}实现.
 */
class DefaultCacheMethodDetails<A extends Annotation> implements CacheMethodDetails<A> {

	private final Method method;

	private final Set<Annotation> annotations;

	private final A cacheAnnotation;

	private final String cacheName;


	public DefaultCacheMethodDetails(Method method, A cacheAnnotation, String cacheName) {
		this.method = method;
		this.annotations = Collections.unmodifiableSet(
				new LinkedHashSet<Annotation>(asList(method.getAnnotations())));
		this.cacheAnnotation = cacheAnnotation;
		this.cacheName = cacheName;
	}


	@Override
	public Method getMethod() {
		return this.method;
	}

	@Override
	public Set<Annotation> getAnnotations() {
		return this.annotations;
	}

	@Override
	public A getCacheAnnotation() {
		return this.cacheAnnotation;
	}

	@Override
	public String getCacheName() {
		return this.cacheName;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("CacheMethodDetails[");
		sb.append("method=").append(this.method);
		sb.append(", cacheAnnotation=").append(this.cacheAnnotation);
		sb.append(", cacheName='").append(this.cacheName).append('\'');
		sb.append(']');
		return sb.toString();
	}

}
