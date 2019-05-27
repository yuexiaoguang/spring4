package org.springframework.aop;

/**
 * 核心Spring切点抽象.
 *
 * <p>切点由{@link ClassFilter}和{@link MethodMatcher}组成.
 * 这些基本术语和Pointcut本身都可以组合起来构建组合 (e.g. 通过 {@link org.springframework.aop.support.ComposablePointcut}).
 */
public interface Pointcut {

	/**
	 * 返回此切点的ClassFilter.
	 * 
	 * @return ClassFilter (永远不会是{@code null})
	 */
	ClassFilter getClassFilter();

	/**
	 * 返回此切点的MethodMatcher.
	 * 
	 * @return MethodMatcher (永远不会是{@code null})
	 */
	MethodMatcher getMethodMatcher();


	/**
	 * 总是匹配的Pointcut实例.
	 */
	Pointcut TRUE = TruePointcut.INSTANCE;

}
