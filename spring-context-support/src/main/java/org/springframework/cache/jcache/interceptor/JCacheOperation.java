package org.springframework.cache.jcache.interceptor;

import java.lang.annotation.Annotation;
import javax.cache.annotation.CacheInvocationParameter;
import javax.cache.annotation.CacheMethodDetails;

import org.springframework.cache.interceptor.BasicOperation;
import org.springframework.cache.interceptor.CacheResolver;

/**
 * 建模JSR-107缓存操作的基础.
 * <p>缓存操作可以静态缓存, 因为它不包含特定缓存调用的任何运行时操作.
 *
 * @param <A> JSR-107注解的类型
 */
public interface JCacheOperation<A extends Annotation> extends BasicOperation, CacheMethodDetails<A> {

	/**
	 * 返回解析用于此操作的缓存的{@link CacheResolver}实例.
	 */
	CacheResolver getCacheResolver();

	/**
	 * 根据指定的方法参数返回{@link CacheInvocationParameter}实例.
	 * <p>方法参数必须与相关方法调用的签名匹配
	 * 
	 * @param values 特定调用的参数值
	 */
	CacheInvocationParameter[] getAllParameters(Object... values);

}