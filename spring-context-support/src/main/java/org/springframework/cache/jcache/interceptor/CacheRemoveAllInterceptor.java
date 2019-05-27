package org.springframework.cache.jcache.interceptor;

import javax.cache.annotation.CacheRemoveAll;

import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.CacheOperationInvoker;

/**
 * 拦截使用{@link CacheRemoveAll}注解的方法.
 */
@SuppressWarnings("serial")
class CacheRemoveAllInterceptor
		extends AbstractCacheInterceptor<CacheRemoveAllOperation, CacheRemoveAll> {

	protected CacheRemoveAllInterceptor(CacheErrorHandler errorHandler) {
		super(errorHandler);
	}

	@Override
	protected Object invoke(CacheOperationInvocationContext<CacheRemoveAllOperation> context,
			CacheOperationInvoker invoker) {

		CacheRemoveAllOperation operation = context.getOperation();

		boolean earlyRemove = operation.isEarlyRemove();

		if (earlyRemove) {
			removeAll(context);
		}

		try {
			Object result = invoker.invoke();
			if (!earlyRemove) {
				removeAll(context);
			}
			return result;
		}
		catch (CacheOperationInvoker.ThrowableWrapper ex) {
			Throwable original = ex.getOriginal();
			if (!earlyRemove && operation.getExceptionTypeFilter().match(original.getClass())) {
				removeAll(context);
			}
			throw ex;
		}
	}

	protected void removeAll(CacheOperationInvocationContext<CacheRemoveAllOperation> context) {
		Cache cache = resolveCache(context);
		if (logger.isTraceEnabled()) {
			logger.trace("Invalidating entire cache '" + cache.getName() + "' for operation "
					+ context.getOperation());
		}
		doClear(cache);
	}

}
