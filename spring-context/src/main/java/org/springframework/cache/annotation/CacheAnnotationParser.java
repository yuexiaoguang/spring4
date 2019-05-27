package org.springframework.cache.annotation;

import java.lang.reflect.Method;
import java.util.Collection;

import org.springframework.cache.interceptor.CacheOperation;

/**
 * 用于解析已知缓存注解类型的策略接口.
 * {@link AnnotationCacheOperationSource}委托给此类解析器, 用于支持特定的注解类型,
 * 例如Spring自己的 {@link Cacheable}, {@link CachePut}, {@link CacheEvict}.
 */
public interface CacheAnnotationParser {

	/**
	 * 根据此解析器理解的注解类型, 解析给定类的缓存定义.
	 * <p>这实际上将已知的缓存注解解析为Spring的元数据属性类. 如果类不可缓存, 则返回{@code null}.
	 * 
	 * @param type 被注解的类
	 * 
	 * @return 配置的缓存操作, 或{@code null}
	 */
	Collection<CacheOperation> parseCacheAnnotations(Class<?> type);

	/**
	 * 基于此解析器理解的注解类型, 解析给定方法的缓存定义.
	 * <p>这实际上将已知的缓存注解解析为Spring的元数据属性类. 如果方法不可缓存, 则返回{@code null}.
	 * 
	 * @param method 被注解的方法
	 * 
	 * @return 配置的缓存操作, 或{@code null}
	 */
	Collection<CacheOperation> parseCacheAnnotations(Method method);

}
