package org.springframework.cache.jcache.config;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.cache.annotation.AbstractCachingConfiguration;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.jcache.interceptor.DefaultJCacheOperationSource;
import org.springframework.cache.jcache.interceptor.JCacheOperationSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;

/**
 * 抽象的JSR-107特定的{@code @Configuration}类, 提供了用于启用JSR-107注解驱动的缓存管理功能的通用结构.
 */
@Configuration
public class AbstractJCacheConfiguration extends AbstractCachingConfiguration {

	protected CacheResolver exceptionCacheResolver;

	@Override
	protected void useCachingConfigurer(CachingConfigurer config) {
		super.useCachingConfigurer(config);
		if (config instanceof JCacheConfigurer) {
			this.exceptionCacheResolver = ((JCacheConfigurer) config).exceptionCacheResolver();
		}
	}

	@Bean(name = "jCacheOperationSource")
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public JCacheOperationSource cacheOperationSource() {
		DefaultJCacheOperationSource source = new DefaultJCacheOperationSource();
		if (this.cacheManager != null) {
			source.setCacheManager(this.cacheManager);
		}
		if (this.keyGenerator != null) {
			source.setKeyGenerator(this.keyGenerator);
		}
		if (this.cacheResolver != null) {
			source.setCacheResolver(this.cacheResolver);
		}
		if (this.exceptionCacheResolver != null) {
			source.setExceptionCacheResolver(this.exceptionCacheResolver);
		}
		return source;
	}

}
