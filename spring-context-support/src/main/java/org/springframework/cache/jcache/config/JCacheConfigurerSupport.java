package org.springframework.cache.jcache.config;

import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.interceptor.CacheResolver;

/**
 * {@link CachingConfigurerSupport}的扩展, 它还实现了{@link JCacheConfigurer}.
 *
 * <p>JSR-107注解的用户可以从此类扩展, 而不是直接从{@link JCacheConfigurer}实现.
 */
public class JCacheConfigurerSupport extends CachingConfigurerSupport implements JCacheConfigurer {

	@Override
	public CacheResolver exceptionCacheResolver() {
		return null;
	}

}
