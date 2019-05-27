package org.springframework.cache.jcache.interceptor;

import javax.cache.annotation.CacheResult;

import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.CacheOperationInvoker;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.util.ExceptionTypeFilter;
import org.springframework.util.SerializationUtils;

/**
 * 拦截使用{@link CacheResult}注解的方法.
 */
@SuppressWarnings("serial")
class CacheResultInterceptor extends AbstractKeyCacheInterceptor<CacheResultOperation, CacheResult> {

	public CacheResultInterceptor(CacheErrorHandler errorHandler) {
		super(errorHandler);
	}


	@Override
	protected Object invoke(CacheOperationInvocationContext<CacheResultOperation> context,
			CacheOperationInvoker invoker) {

		CacheResultOperation operation = context.getOperation();
		Object cacheKey = generateKey(context);

		Cache cache = resolveCache(context);
		Cache exceptionCache = resolveExceptionCache(context);

		if (!operation.isAlwaysInvoked()) {
			Cache.ValueWrapper cachedValue = doGet(cache, cacheKey);
			if (cachedValue != null) {
				return cachedValue.get();
			}
			checkForCachedException(exceptionCache, cacheKey);
		}

		try {
			Object invocationResult = invoker.invoke();
			doPut(cache, cacheKey, invocationResult);
			return invocationResult;
		}
		catch (CacheOperationInvoker.ThrowableWrapper ex) {
			Throwable original = ex.getOriginal();
			cacheException(exceptionCache, operation.getExceptionTypeFilter(), cacheKey, original);
			throw ex;
		}
	}

	/**
	 * 检查缓存的异常. 如果发现异常, 直接抛出.
	 */
	protected void checkForCachedException(Cache exceptionCache, Object cacheKey) {
		if (exceptionCache == null) {
			return;
		}
		Cache.ValueWrapper result = doGet(exceptionCache, cacheKey);
		if (result != null) {
			throw rewriteCallStack((Throwable) result.get(), getClass().getName(), "invoke");
		}
	}

	protected void cacheException(Cache exceptionCache, ExceptionTypeFilter filter, Object cacheKey, Throwable ex) {
		if (exceptionCache == null) {
			return;
		}
		if (filter.match(ex.getClass())) {
			doPut(exceptionCache, cacheKey, ex);
		}
	}

	private Cache resolveExceptionCache(CacheOperationInvocationContext<CacheResultOperation> context) {
		CacheResolver exceptionCacheResolver = context.getOperation().getExceptionCacheResolver();
		if (exceptionCacheResolver != null) {
			return extractFrom(context.getOperation().getExceptionCacheResolver().resolveCaches(context));
		}
		return null;
	}


	/**
	 * 重写指定的{@code exception}的调用堆栈, 使其与当前调用堆栈匹配, 直到 (包括)指定的方法调用.
	 * <p>克隆指定的异常. 如果异常不是{@code serializable}, 则返回原始异常.
	 * 如果找不到共同的祖先, 则返回原始异常.
	 * <p>用于确保缓存的异常具有有效的调用上下文.
	 * 
	 * @param exception 与当前调用堆栈合并的异常
	 * @param className 共同祖先的类名
	 * @param methodName 共同祖先的方法名称
	 * 
	 * @return 带有重写调用堆栈的克隆异常, 该堆栈由当前调用堆栈组成, 
	 * 最多包含(包括){@code className}和{@code methodName}参数指定的公共祖先,
	 * 在共同的祖先之后是指定的{@code exception}的堆栈跟踪元素.
	 */
	private static CacheOperationInvoker.ThrowableWrapper rewriteCallStack(
			Throwable exception, String className, String methodName) {

		Throwable clone = cloneException(exception);
		if (clone == null) {
			return new CacheOperationInvoker.ThrowableWrapper(exception);
		}

		StackTraceElement[] callStack = new Exception().getStackTrace();
		StackTraceElement[] cachedCallStack = exception.getStackTrace();

		int index = findCommonAncestorIndex(callStack, className, methodName);
		int cachedIndex = findCommonAncestorIndex(cachedCallStack, className, methodName);
		if (index == -1 || cachedIndex == -1) {
			return new CacheOperationInvoker.ThrowableWrapper(exception); // Cannot find common ancestor
		}
		StackTraceElement[] result = new StackTraceElement[cachedIndex + callStack.length - index];
		System.arraycopy(cachedCallStack, 0, result, 0, cachedIndex);
		System.arraycopy(callStack, index, result, cachedIndex, callStack.length - index);

		clone.setStackTrace(result);
		return new CacheOperationInvoker.ThrowableWrapper(clone);
	}

	@SuppressWarnings("unchecked")
	private static <T extends Throwable> T cloneException(T exception) {
		try {
			return (T) SerializationUtils.deserialize(SerializationUtils.serialize(exception));
		}
		catch (Exception ex) {
			return null;  // exception parameter cannot be cloned
		}
	}

	private static int findCommonAncestorIndex(StackTraceElement[] callStack, String className, String methodName) {
		for (int i = 0; i < callStack.length; i++) {
			StackTraceElement element = callStack[i];
			if (className.equals(element.getClassName()) && methodName.equals(element.getMethodName())) {
				return i;
			}
		}
		return -1;
	}

}
