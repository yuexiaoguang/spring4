package org.springframework.aop.support;

import java.lang.reflect.Method;

import org.springframework.aop.MethodMatcher;

/**
 * 动态方法匹配器的方便的抽象超类, 在运行时关注参数.
 */
public abstract class DynamicMethodMatcher implements MethodMatcher {

	@Override
	public final boolean isRuntime() {
		return true;
	}

	/**
	 * 可以覆盖以添加动态匹配的前提条件. 此实现总是返回 true.
	 */
	@Override
	public boolean matches(Method method, Class<?> targetClass) {
		return true;
	}

}
