package org.springframework.cache.jcache.config;

import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheResolver;

/**
 * JSR-107实现的{@link CachingConfigurer}扩展.
 *
 * <p>由带有{@link org.springframework.cache.annotation.EnableCaching}注解的类实现,
 * 这些类希望或需要明确指定, 如何为注解驱动的缓存管理解析异常缓存.
 * 考虑扩展{@link JCacheConfigurerSupport}, 它提供所有接口方法的stub实现.
 *
 * <p>有关一般示例和上下文, 请参阅{@link org.springframework.cache.annotation.EnableCaching};
 * 有关详细说明, 请参阅{@link #exceptionCacheResolver()}.
 */
public interface JCacheConfigurer extends CachingConfigurer {

	/**
	 * 返回{@link CacheResolver} bean, 用于解析注解驱动的缓存管理的异常缓存.
	 * 实现必须明确声明{@link org.springframework.context.annotation.Bean @Bean}, e.g.
	 * <pre class="code">
	 * &#064;Configuration
	 * &#064;EnableCaching
	 * public class AppConfig extends JCacheConfigurerSupport {
	 *     &#064;Bean // important!
	 *     &#064;Override
	 *     public CacheResolver exceptionCacheResolver() {
	 *         // configure and return CacheResolver instance
	 *     }
	 *     // ...
	 * }
	 * </pre>
	 * 有关更完整的示例, 请参阅{@link org.springframework.cache.annotation.EnableCaching}.
	 */
	CacheResolver exceptionCacheResolver();

}
