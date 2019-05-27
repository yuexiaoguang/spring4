package org.springframework.cache.jcache.interceptor;

import javax.cache.annotation.CacheMethodDetails;
import javax.cache.annotation.CacheResult;

import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.util.ExceptionTypeFilter;
import org.springframework.util.StringUtils;

/**
 * {@link CacheResult}操作的{@link JCacheOperation}实现.
 */
class CacheResultOperation extends AbstractJCacheKeyOperation<CacheResult> {

	private final ExceptionTypeFilter exceptionTypeFilter;

	private final CacheResolver exceptionCacheResolver;

	private final String exceptionCacheName;


	public CacheResultOperation(CacheMethodDetails<CacheResult> methodDetails, CacheResolver cacheResolver,
			KeyGenerator keyGenerator, CacheResolver exceptionCacheResolver) {

		super(methodDetails, cacheResolver, keyGenerator);
		CacheResult ann = methodDetails.getCacheAnnotation();
		this.exceptionTypeFilter = createExceptionTypeFilter(ann.cachedExceptions(), ann.nonCachedExceptions());
		this.exceptionCacheResolver = exceptionCacheResolver;
		this.exceptionCacheName = (StringUtils.hasText(ann.exceptionCacheName()) ? ann.exceptionCacheName() : null);
	}


	@Override
	public ExceptionTypeFilter getExceptionTypeFilter() {
		return this.exceptionTypeFilter;
	}

	/**
	 * 指定是否应始终调用该方法, 而不管缓存是否命中.
	 * 默认情况下, 仅在缓存未命中时才调用该方法.
	 */
	public boolean isAlwaysInvoked() {
		return getCacheAnnotation().skipGet();
	}

	/**
	 * 返回用于解析缓存的{@link CacheResolver}实例, 以用于匹配此操作引发的异常.
	 */
	public CacheResolver getExceptionCacheResolver() {
		return this.exceptionCacheResolver;
	}

	/**
	 * 返回缓存名称以缓存异常; 如果应禁用缓存异常, 则返回{@code null}.
	 */
	public String getExceptionCacheName() {
		return this.exceptionCacheName;
	}

}
