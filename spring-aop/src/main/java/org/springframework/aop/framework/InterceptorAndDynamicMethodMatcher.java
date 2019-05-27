package org.springframework.aop.framework;

import org.aopalliance.intercept.MethodInterceptor;

import org.springframework.aop.MethodMatcher;

/**
 * 内部框架类, 将MethodInterceptor实例与MethodMatcher组合以用作切面链中的元素.
 */
class InterceptorAndDynamicMethodMatcher {

	final MethodInterceptor interceptor;

	final MethodMatcher methodMatcher;

	public InterceptorAndDynamicMethodMatcher(MethodInterceptor interceptor, MethodMatcher methodMatcher) {
		this.interceptor = interceptor;
		this.methodMatcher = methodMatcher;
	}

}
