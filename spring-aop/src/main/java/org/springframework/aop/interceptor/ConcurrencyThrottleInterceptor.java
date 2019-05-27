package org.springframework.aop.interceptor;

import java.io.Serializable;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.util.ConcurrencyThrottleSupport;

/**
 * 限制并发访问的拦截器, 如果达到指定的并发限制, 则阻塞调用.
 *
 * <p>可以应用于涉及大量使用系统资源的本地服务方法, 在这种情况下, 更有效地限制特定服务的并发性, 而不是限制整个线程池
 * (e.g. Web容器的线程池).
 *
 * <p>此拦截器的默认并发限制为1. 指定"concurrencyLimit" bean属性来修改这个值.
 */
@SuppressWarnings("serial")
public class ConcurrencyThrottleInterceptor extends ConcurrencyThrottleSupport
		implements MethodInterceptor, Serializable {

	public ConcurrencyThrottleInterceptor() {
		setConcurrencyLimit(1);
	}

	@Override
	public Object invoke(MethodInvocation methodInvocation) throws Throwable {
		beforeAccess();
		try {
			return methodInvocation.proceed();
		}
		finally {
			afterAccess();
		}
	}

}
