package org.springframework.cache.annotation;

import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.KeyGenerator;

/**
 * 使用空方法实现{@link CachingConfigurer}, 允许子类仅覆盖他们感兴趣的方法.
 */
public class CachingConfigurerSupport implements CachingConfigurer {

	@Override
	public CacheManager cacheManager() {
		return null;
	}

	@Override
	public CacheResolver cacheResolver() {
		return null;
	}

	@Override
	public KeyGenerator keyGenerator() {
		return null;
	}

	@Override
	public CacheErrorHandler errorHandler() {
		return null;
	}

}
