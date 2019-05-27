package org.springframework.cache.jcache.interceptor;

import javax.cache.annotation.CacheRemove;

import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.CacheOperationInvoker;

/**
 * 拦截使用{@link CacheRemove}注解的方法.
 */
@SuppressWarnings("serial")
class CacheRemoveEntryInterceptor extends AbstractKeyCacheInterceptor<CacheRemoveOperation, CacheRemove> {

	protected CacheRemoveEntryInterceptor(CacheErrorHandler errorHandler) {
		super(errorHandler);
	}

	@Override
	protected Object invoke(CacheOperationInvocationContext<CacheRemoveOperation> context,
			CacheOperationInvoker invoker) {
		CacheRemoveOperation operation = context.getOperation();

		final boolean earlyRemove = operation.isEarlyRemove();

		if (earlyRemove) {
			removeValue(context);
		}

		try {
			Object result = invoker.invoke();
			if (!earlyRemove) {
				removeValue(context);
			}
			return result;
		}
		catch (CacheOperationInvoker.ThrowableWrapper t) {
			Throwable ex = t.getOriginal();
			if (!earlyRemove && operation.getExceptionTypeFilter().match(ex.getClass())) {
				removeValue(context);
			}
			throw t;
		}
	}

	private void removeValue(CacheOperationInvocationContext<CacheRemoveOperation> context) {
		Object key = generateKey(context);
		Cache cache = resolveCache(context);
		if (logger.isTraceEnabled()) {
			logger.trace("Invalidating key [" + key + "] on cache '" + cache.getName()
					+ "' for operation " + context.getOperation());
		}
		doEvict(cache, key);
	}

}
