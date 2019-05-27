package org.springframework.cache.config;

/**
 * 跨子包进行内部共享的配置常量.
 */
public class CacheManagementConfigUtils {

	public static final String CACHE_ADVISOR_BEAN_NAME =
			"org.springframework.cache.config.internalCacheAdvisor";

	public static final String CACHE_ASPECT_BEAN_NAME =
			"org.springframework.cache.config.internalCacheAspect";

	public static final String JCACHE_ADVISOR_BEAN_NAME =
			"org.springframework.cache.config.internalJCacheAdvisor";

	public static final String JCACHE_ASPECT_BEAN_NAME =
			"org.springframework.cache.config.internalJCacheAspect";

}
