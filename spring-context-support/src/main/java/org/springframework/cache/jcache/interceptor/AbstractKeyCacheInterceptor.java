package org.springframework.cache.jcache.interceptor;

import java.lang.annotation.Annotation;
import javax.cache.annotation.CacheKeyInvocationContext;

import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.KeyGenerator;

/**
 * JSR-107基于Key的缓存注解的基本拦截器.
 */
@SuppressWarnings("serial")
abstract class AbstractKeyCacheInterceptor<O extends AbstractJCacheKeyOperation<A>, A extends Annotation>
		extends AbstractCacheInterceptor<O, A> {

	protected AbstractKeyCacheInterceptor(CacheErrorHandler errorHandler) {
		super(errorHandler);
	}

	/**
	 * 为指定的调用生成Key.
	 * 
	 * @param context 调用的上下文
	 * 
	 * @return 要使用的key
	 */
	protected Object generateKey(CacheOperationInvocationContext<O> context) {
		KeyGenerator keyGenerator = context.getOperation().getKeyGenerator();
		Object key = keyGenerator.generate(context.getTarget(), context.getMethod(), context.getArgs());
		if (logger.isTraceEnabled()) {
			logger.trace("Computed cache key " + key + " for operation " + context.getOperation());
		}
		return key;
	}

	/**
	 * 根据指定的调用创建{@link CacheKeyInvocationContext}.
	 * 
	 * @param context 调用的上下文
	 * 
	 * @return 相关的{@code CacheKeyInvocationContext}
	 */
	protected CacheKeyInvocationContext<A> createCacheKeyInvocationContext(
			CacheOperationInvocationContext<O> context) {
		return new DefaultCacheKeyInvocationContext<A>(context.getOperation(), context.getTarget(), context.getArgs());
	}

}
