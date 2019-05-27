package org.springframework.cache.aspectj;

import java.lang.reflect.Method;

import org.aspectj.lang.annotation.SuppressAjWarnings;
import org.aspectj.lang.reflect.MethodSignature;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.cache.interceptor.CacheAspectSupport;
import org.springframework.cache.interceptor.CacheOperationInvoker;
import org.springframework.cache.interceptor.CacheOperationSource;

/**
 * Abstract superaspect for AspectJ cache aspects. Concrete subaspects will implement the
 * {@link #cacheMethodExecution} pointcut using a strategy such as Java 5 annotations.
 *
 * <p>Suitable for use inside or outside the Spring IoC container. Set the
 * {@link #setCacheManager cacheManager} property appropriately, allowing use of any cache
 * implementation supported by Spring.
 *
 * <p><b>NB:</b> If a method implements an interface that is itself cache annotated, the
 * relevant Spring cache definition will <i>not</i> be resolved.
 */
public abstract aspect AbstractCacheAspect extends CacheAspectSupport implements DisposableBean {

	protected AbstractCacheAspect() {
	}

	/**
	 * Construct object using the given caching metadata retrieval strategy.
	 * @param cos {@link CacheOperationSource} implementation, retrieving Spring cache
	 * metadata for each joinpoint.
	 */
	protected AbstractCacheAspect(CacheOperationSource... cos) {
		setCacheOperationSources(cos);
	}

	@Override
	public void destroy() {
		clearMetadataCache(); // An aspect is basically a singleton
	}

	@SuppressAjWarnings("adviceDidNotMatch")
	Object around(final Object cachedObject) : cacheMethodExecution(cachedObject) {
		MethodSignature methodSignature = (MethodSignature) thisJoinPoint.getSignature();
		Method method = methodSignature.getMethod();

		CacheOperationInvoker aspectJInvoker = new CacheOperationInvoker() {
			public Object invoke() {
				try {
					return proceed(cachedObject);
				}
				catch (Throwable ex) {
					throw new ThrowableWrapper(ex);
				}
			}
		};

		try {
			return execute(aspectJInvoker, thisJoinPoint.getTarget(), method, thisJoinPoint.getArgs());
		}
		catch (CacheOperationInvoker.ThrowableWrapper th) {
			AnyThrow.throwUnchecked(th.getOriginal());
			return null; // never reached
		}
	}

	/**
	 * Concrete subaspects must implement this pointcut, to identify cached methods.
	 */
	protected abstract pointcut cacheMethodExecution(Object cachedObject);

}
