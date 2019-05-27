package org.springframework.cache.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @CacheConfig} 提供了一种在类级别共享与公共缓存相关的设置的机制.
 *
 * <p>当此注解出现在给定类上时, 它为该类中定义的缓存操作提供了一组默认设置.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CacheConfig {

	/**
	 * 默认缓存的名称.
	 * <p>如果没有在操作级别设置, 则使用这些而不是默认值.
	 * <p>可用于确定目标缓存, 匹配特定bean定义的限定符值或bean名称.
	 */
	String[] cacheNames() default {};

	/**
	 * 用于该类的默认 {@link org.springframework.cache.interceptor.KeyGenerator} 的bean名称.
	 * <p>如果没有在操作级别设置, 则使用此一个而不是默认值.
	 * <p>Key生成器与使用自定义Key互斥. 为操作定义此键时, 将忽略此键生成器的值.
	 */
	String keyGenerator() default "";

	/**
	 * 自定义 {@link org.springframework.cache.CacheManager}的bean名称,
	 * 用于创建默认的 {@link org.springframework.cache.interceptor.CacheResolver}, 如果尚未设置.
	 * <p>如果在操作级别没有设置解析器和缓存管理器, 并且没有通过{@link #cacheResolver}设置缓存解析器, 则使用此解析器而不是默认值.
	 */
	String cacheManager() default "";

	/**
	 * 要使用的自定义{@link org.springframework.cache.interceptor.CacheResolver}的bean名称.
	 * <p>如果在操作级别没有设置解析器和缓存管理器, 则使用此解析器而不是默认值.
	 */
	String cacheResolver() default "";

}
