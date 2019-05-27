package org.springframework.cache.jcache.interceptor;

import javax.cache.annotation.CacheMethodDetails;
import javax.cache.annotation.CacheRemove;

import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.util.ExceptionTypeFilter;

/**
 * {@link CacheRemove}操作的{@link JCacheOperation}实现.
 */
class CacheRemoveOperation extends AbstractJCacheKeyOperation<CacheRemove> {

	private final ExceptionTypeFilter exceptionTypeFilter;


	public CacheRemoveOperation(
			CacheMethodDetails<CacheRemove> methodDetails, CacheResolver cacheResolver, KeyGenerator keyGenerator) {

		super(methodDetails, cacheResolver, keyGenerator);
		CacheRemove ann = methodDetails.getCacheAnnotation();
		this.exceptionTypeFilter = createExceptionTypeFilter(ann.evictFor(), ann.noEvictFor());
	}


	@Override
	public ExceptionTypeFilter getExceptionTypeFilter() {
		return this.exceptionTypeFilter;
	}

	/**
	 * 指定在调用方法之前是否应删除缓存条目.
	 * 默认情况下, 在方法调用后删除缓存条目.
	 */
	public boolean isEarlyRemove() {
		return !getCacheAnnotation().afterInvocation();
	}

}
