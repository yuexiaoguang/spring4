package org.springframework.cache.aspectj;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.cache.config.CacheManagementConfigUtils;
import org.springframework.cache.jcache.config.AbstractJCacheConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;

/**
 * {@code @Configuration}类，用于为标准JSR-107注解注册启用基于AspectJ注解驱动的缓存管理所必需的Spring基础结构bean.
 */
@Configuration
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class AspectJJCacheConfiguration extends AbstractJCacheConfiguration {

	@Bean(name = CacheManagementConfigUtils.JCACHE_ASPECT_BEAN_NAME)
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public JCacheCacheAspect cacheAspect() {
		JCacheCacheAspect cacheAspect = JCacheCacheAspect.aspectOf();
		cacheAspect.setCacheOperationSource(cacheOperationSource());
		return cacheAspect;
	}

}
