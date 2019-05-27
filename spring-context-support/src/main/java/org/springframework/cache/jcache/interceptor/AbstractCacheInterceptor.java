package org.springframework.cache.jcache.interceptor;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.AbstractCacheInvoker;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.CacheOperationInvoker;
import org.springframework.util.CollectionUtils;

/**
 * JSR-107缓存注解的基本拦截器.
 */
@SuppressWarnings("serial")
abstract class AbstractCacheInterceptor<O extends AbstractJCacheOperation<A>, A extends Annotation>
		extends AbstractCacheInvoker implements Serializable {

	protected final Log logger = LogFactory.getLog(getClass());


	protected AbstractCacheInterceptor(CacheErrorHandler errorHandler) {
		super(errorHandler);
	}


	protected abstract Object invoke(CacheOperationInvocationContext<O> context, CacheOperationInvoker invoker)
			throws Throwable;


	/**
	 * 解析要使用的缓存.
	 * 
	 * @param context 调用上下文
	 * 
	 * @return 要使用的缓存 (never null)
	 */
	protected Cache resolveCache(CacheOperationInvocationContext<O> context) {
		Collection<? extends Cache> caches = context.getOperation().getCacheResolver().resolveCaches(context);
		Cache cache = extractFrom(caches);
		if (cache == null) {
			throw new IllegalStateException("Cache could not have been resolved for " + context.getOperation());
		}
		return cache;
	}

	/**
	 * 在单个期望元素中转换缓存集合.
	 * <p>如果集合包含多个元素, 则抛出{@link IllegalStateException}
	 * 
	 * @return 单个元素, 或{@code null}如果集合为空
	 */
	static Cache extractFrom(Collection<? extends Cache> caches) {
		if (CollectionUtils.isEmpty(caches)) {
			return null;
		}
		else if (caches.size() == 1) {
			return caches.iterator().next();
		}
		else {
			throw new IllegalStateException("Unsupported cache resolution result " + caches +
					": JSR-107 only supports a single cache.");
		}
	}

}
