package org.springframework.cache.jcache.interceptor;

import javax.cache.annotation.CacheMethodDetails;
import javax.cache.annotation.CacheRemoveAll;

import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.util.ExceptionTypeFilter;

/**
 * {@link CacheRemoveAll}操作的{@link JCacheOperation}实现.
 */
class CacheRemoveAllOperation extends AbstractJCacheOperation<CacheRemoveAll> {

	private final ExceptionTypeFilter exceptionTypeFilter;


	public CacheRemoveAllOperation(CacheMethodDetails<CacheRemoveAll> methodDetails, CacheResolver cacheResolver) {
		super(methodDetails, cacheResolver);
		CacheRemoveAll ann = methodDetails.getCacheAnnotation();
		this.exceptionTypeFilter = createExceptionTypeFilter(ann.evictFor(), ann.noEvictFor());
	}


	@Override
	public ExceptionTypeFilter getExceptionTypeFilter() {
		return this.exceptionTypeFilter;
	}

	/**
	 * 指定在调用方法之前是否应清除缓存.
	 * 默认情况下, 在方法调用后清除缓存.
	 */
	public boolean isEarlyRemove() {
		return !getCacheAnnotation().afterInvocation();
	}

}
