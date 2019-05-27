package org.springframework.cache.annotation;

import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.KeyGenerator;

/**
 * 由带@{@link EnableCaching}注解的@{@link org.springframework.context.annotation.Configuration Configuration}类实现的接口,
 * 希望或需要明确指定如何解析缓存, 以及如何为注解驱动的缓存管理生成Key.
 * 考虑扩展{@link CachingConfigurerSupport}, 它提供所有接口方法的stub实现.
 *
 * <p>有关一般示例和上下文, 请参阅 @{@link EnableCaching};
 * 有关详细说明, 请参阅 {@link #cacheManager()}, {@link #cacheResolver()}, {@link #keyGenerator()}.
 */
public interface CachingConfigurer {

	/**
	 * 返回用于注释驱动的缓存管理的缓存管理器bean.
	 * 使用此缓存管理器将在后台初始化默认的{@link CacheResolver}.
	 * 要更精细地管理缓存解析, 请考虑直接设置{@link CacheResolver}.
	 * <p>实现必须明确声明 {@link org.springframework.context.annotation.Bean @Bean}, e.g.
	 * <pre class="code">
	 * &#064;Configuration
	 * &#064;EnableCaching
	 * public class AppConfig extends CachingConfigurerSupport {
	 *     &#064;Bean // important!
	 *     &#064;Override
	 *     public CacheManager cacheManager() {
	 *         // configure and return CacheManager instance
	 *     }
	 *     // ...
	 * }
	 * </pre>
	 * 有关更完整的示例, 请参阅 @{@link EnableCaching}.
	 */
	CacheManager cacheManager();

	/**
	 * 返回{@link CacheResolver} bean, 用于解析注解驱动的缓存管理的常规缓存.
	 * 这是指定要使用的{@link CacheManager}的替代且更强大的选项.
	 * <p>如果同时设置了 {@link #cacheManager()}和{@code #cacheResolver()}, 则忽略缓存管理器.
	 * <p>实现必须明确声明 {@link org.springframework.context.annotation.Bean @Bean}, e.g.
	 * <pre class="code">
	 * &#064;Configuration
	 * &#064;EnableCaching
	 * public class AppConfig extends CachingConfigurerSupport {
	 *     &#064;Bean // important!
	 *     &#064;Override
	 *     public CacheResolver cacheResolver() {
	 *         // configure and return CacheResolver instance
	 *     }
	 *     // ...
	 * }
	 * </pre>
	 * 有关更完整的示例, 请参阅 @{@link EnableCaching}.
	 */
	CacheResolver cacheResolver();

	/**
	 * 返回用于注解驱动的缓存管理的Key生成器bean.
	 * 实现必须明确声明 {@link org.springframework.context.annotation.Bean @Bean}, e.g.
	 * <pre class="code">
	 * &#064;Configuration
	 * &#064;EnableCaching
	 * public class AppConfig extends CachingConfigurerSupport {
	 *     &#064;Bean // important!
	 *     &#064;Override
	 *     public KeyGenerator keyGenerator() {
	 *         // configure and return KeyGenerator instance
	 *     }
	 *     // ...
	 * }
	 * </pre>
	 * 有关更完整的示例, 请参阅 @{@link EnableCaching}.
	 */
	KeyGenerator keyGenerator();

	/**
	 * 返回用于处理与缓存相关的错误的{@link CacheErrorHandler}.
	 * <p>默认情况下, 使用{@link org.springframework.cache.interceptor.SimpleCacheErrorHandler}并简单地将异常抛回客户端.
	 * <p>实现必须明确声明 {@link org.springframework.context.annotation.Bean @Bean}, e.g.
	 * <pre class="code">
	 * &#064;Configuration
	 * &#064;EnableCaching
	 * public class AppConfig extends CachingConfigurerSupport {
	 *     &#064;Bean // important!
	 *     &#064;Override
	 *     public CacheErrorHandler errorHandler() {
	 *         // configure and return CacheErrorHandler instance
	 *     }
	 *     // ...
	 * }
	 * </pre>
	 * 有关更完整的示例, 请参阅 @{@link EnableCaching}.
	 */
	CacheErrorHandler errorHandler();

}
