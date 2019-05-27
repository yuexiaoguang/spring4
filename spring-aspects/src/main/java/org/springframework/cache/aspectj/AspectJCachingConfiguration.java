package org.springframework.cache.aspectj;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.cache.annotation.AbstractCachingConfiguration;
import org.springframework.cache.config.CacheManagementConfigUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;

/**
 * {@code @Configuration}类, 用于注册启用基于AspectJ注解驱动的缓存管理所必需的Spring基础结构bean.
 */
@Configuration
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class AspectJCachingConfiguration extends AbstractCachingConfiguration {

	@Bean(name = CacheManagementConfigUtils.CACHE_ASPECT_BEAN_NAME)
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public AnnotationCacheAspect cacheAspect() {
		AnnotationCacheAspect cacheAspect = AnnotationCacheAspect.aspectOf();
		if (this.cacheResolver != null) {
			cacheAspect.setCacheResolver(this.cacheResolver);
		}
		else if (this.cacheManager != null) {
			cacheAspect.setCacheManager(this.cacheManager);
		}
		if (this.keyGenerator != null) {
			cacheAspect.setKeyGenerator(this.keyGenerator);
		}
		if (this.errorHandler != null) {
			cacheAspect.setErrorHandler(this.errorHandler);
		}
		return cacheAspect;
	}

}
