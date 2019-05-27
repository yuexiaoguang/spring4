package org.springframework.cache.jcache.interceptor;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.cache.interceptor.CacheOperationInvoker;

/**
 * AOP Alliance MethodInterceptor, 用于使用JSR-107缓存注解进行声明性缓存管理.
 *
 * <p>派生自{@link JCacheAspectSupport}类, 该类包含与Spring的底层缓存API的集成.
 * JCacheInterceptor只是调用相关的超类方法.
 *
 * <p>JCacheInterceptor是线程安全的.
 */
@SuppressWarnings("serial")
public class JCacheInterceptor extends JCacheAspectSupport implements MethodInterceptor, Serializable {

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
