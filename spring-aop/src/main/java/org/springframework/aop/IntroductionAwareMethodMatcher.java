package org.springframework.aop;

import java.lang.reflect.Method;

/**
 * 一种特殊类型的{@link MethodMatcher}, 在匹配方法时会考虑引入. 如果目标类没有引入, 方法匹配器可能能够更有效地优化匹配.
 */
public interface IntroductionAwareMethodMatcher extends MethodMatcher {

	/**
	 * 执行静态检查给定方法是否匹配.
	 * 如果调用者支持扩展的IntroductionAwareMethodMatcher接口, 将调用这个方法,
	 * 而不是两个参数的 {@link #matches(java.lang.reflect.Method, Class)}方法.
	 * 
	 * @param method 候选方法
	 * @param targetClass 目标类 (可能是 {@code null}, 在这种情况下, 候选类必须被视为方法的声明类)
	 * @param hasIntroductions {@code true}如果所代表的对象是一个或多个引入的主体; 否则{@code false}
	 * 
	 * @return 此方法是否与静态匹配
	 */
	boolean matches(Method method, Class<?> targetClass, boolean hasIntroductions);

}
