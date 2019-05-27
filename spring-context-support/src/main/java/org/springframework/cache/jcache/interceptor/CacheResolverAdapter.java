package org.springframework.cache.jcache.interceptor;

import java.util.Collection;
import java.util.Collections;
import javax.cache.annotation.CacheInvocationContext;

import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.jcache.JCacheCache;
import org.springframework.util.Assert;

/**
 * Spring的{@link CacheResolver}实现, 委托给标准的JSR-107 {@link javax.cache.annotation.CacheResolver}.
 * <p>在内部用于调用基于用户的JSR-107缓存解析器.
 */
class CacheResolverAdapter implements CacheResolver {

	private final javax.cache.annotation.CacheResolver target;


	/**
	 * 使用JSR-107缓存解析器.
	 */
	public CacheResolverAdapter(javax.cache.annotation.CacheResolver target) {
		Assert.notNull(target, "JSR-107 CacheResolver is required");
		this.target = target;
	}


	/**
	 * 返回此实例正在使用的底层{@link javax.cache.annotation.CacheResolver}.
	 */
	protected javax.cache.annotation.CacheResolver getTarget() {
		return target;
	}

	@Override
	public Collection<? extends Cache> resolveCaches(CacheOperationInvocationContext<?> context) {
		if (!(context instanceof CacheInvocationContext<?>)) {
			throw new IllegalStateException("Unexpected context " + context);
		}
		CacheInvocationContext<?> cacheInvocationContext = (CacheInvocationContext<?>) context;
		javax.cache.Cache<Object, Object> cache = this.target.resolveCache(cacheInvocationContext);
		if (cache == null) {
			throw new IllegalStateException("Could not resolve cache for " + context + " using " + this.target);
		}
		return Collections.singleton(new JCacheCache(cache));
	}

}
