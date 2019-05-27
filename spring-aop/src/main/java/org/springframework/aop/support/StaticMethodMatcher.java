package org.springframework.aop.support;

import java.lang.reflect.Method;

import org.springframework.aop.MethodMatcher;

/**
 * 静态方法匹配器的抽象超类, 不关心运行时的参数.
 */
public abstract class StaticMethodMatcher implements MethodMatcher {

	@Override
	public final boolean isRuntime() {
		return false;
	}

	@Override
	public final boolean matches(Method method, Class<?> targetClass, Object... args) {
		// 永远不应该被调用, 因为 isRuntime()返回 false
		throw new UnsupportedOperationException("Illegal MethodMatcher usage");
	}

}
