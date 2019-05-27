package org.springframework.aop.support;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;

/**
 * 当我们想强制子类实现MethodMatcher接口, 但是子类将要成为切点时, 方便的超类.
 * 可以重写getClassFilter() 方法以自定义ClassFilter行为.
 */
public abstract class DynamicMethodMatcherPointcut extends DynamicMethodMatcher implements Pointcut {

	@Override
	public ClassFilter getClassFilter() {
		return ClassFilter.TRUE;
	}

	@Override
	public final MethodMatcher getMethodMatcher() {
		return this;
	}

}
