package org.springframework.cache.interceptor;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * AOP Alliance MethodInterceptor, 用于使用通用Spring缓存基础结构进行声明式的缓存管理
 * ({@link org.springframework.cache.Cache}).
 *
 * <p>派生自{@link CacheAspectSupport}类, 其中包含与Spring的底层缓存API的集成.
 * CacheInterceptor只是以正确的顺序调用相关的超类方法.
 *
 * <p>CacheInterceptors是线程安全的.
 */
@SuppressWarnings("serial")
public class CacheInterceptor extends CacheAspectSupport implements MethodInterceptor, Serializable {

	@Override
	public Object invoke(final MethodInvocation invocation) throws Throwable {
		Method method = invocation.getMethod();

		CacheOperationInvoker aopAllianceInvoker = new CacheOperationInvoker() {
			@Override
			public Object invoke() {
				try {
					return invocation.proceed();
				}
				catch (Throwable ex) {
					throw new ThrowableWrapper(ex);
				}
			}
		};

		try {
			return execute(aopAllianceInvoker, invocation.getThis(), method, invocation.getArguments());
		}
		catch (CacheOperationInvoker.ThrowableWrapper th) {
			throw th.getOriginal();
		}
	}

}
