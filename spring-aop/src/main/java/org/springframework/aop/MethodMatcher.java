package org.springframework.aop;

import java.lang.reflect.Method;

/**
 * {@link Pointcut}的一部分: 检查目标方法是否符合增强的条件.
 *
 * <p>MethodMatcher必须是<b>statically</b>或 <b>runtime</b> (dynamically).
 * 静态匹配调用方法和（可能）方法属性. 动态匹配还使特定调用的参数可用, 而且运行之前的增强的作用应用于连接点.
 *
 * <p>如果实现从它的{@link #isRuntime()}方法返回{@code false}, 评估可以静态进行, 并且对于此方法的所有调用, 结果都是相同的, 不管它们的参数.
 * 意味着如果{@link #isRuntime()}方法返回{@code false}, 将永远不会调用三个参数的{@link #matches(java.lang.reflect.Method, Class, Object[])}方法.
 *
 * <p>如果实现从它的两个参数的{@link #matches(java.lang.reflect.Method, Class)}返回 {@code true}, 而且它的{@link #isRuntime()}方法返回{@code true},
 * 将立即调用三个参数的{@link #matches(java.lang.reflect.Method, Class, Object[])}方法, <i>在每次潜在执行相关增强之前</i>, 决定增强是否应该运行.
 * 所有以前的增强, 比如拦截链中的早期拦截器, 将运行, 因此在评估时, 它们在参数或ThreadLocal状态中产生的任何状态更改都将可用.
 */
public interface MethodMatcher {

	/**
	 * 执行静态检查给定方法是否匹配.
	 * <p>如果返回 {@code false}或 {@link #isRuntime()}方法返回{@code false}, 不会进行运行时检查
	 * (i.e. 不会调用 {@link #matches(java.lang.reflect.Method, Class, Object[])}).
	 * 
	 * @param method 候选方法
	 * @param targetClass 目标类 (将会是 {@code null}, 在这种情况下, 候选类必须被视为方法的声明类)
	 * 
	 * @return 此方法是否静态匹配
	 */
	boolean matches(Method method, Class<?> targetClass);

	/**
	 * 此MethodMatcher是否是动态的, 即, 必须在运行时对{@link #matches(java.lang.reflect.Method, Class, Object[])}方法进行最后调用,
	 * 即使两个参数的matches方法返回 {@code true}?
	 * <p>可以在创建AOP代理时调用, 并且无需在每次方法调用之前再次调用.
	 * 
	 * @return 如果通过静态匹配, 是否需要通过{@link #matches(java.lang.reflect.Method, Class, Object[])}方法进行运行时匹配
	 */
	boolean isRuntime();

	/**
	 * 检查此方法是否存在运行时（动态）匹配，该方法必须静态匹配.
	 * <p>仅当给定方法和目标类的两个参数的matches方法返回{@code true}时, 以及{@link #isRuntime()}方法返回{@code true}时, 才会调用此方法.
	 * 在潜在运行增强之前立即调用, 在增强链早期的增强运行之后.
	 * 
	 * @param method 候选方法
	 * @param targetClass 目标类 (将会是 {@code null}, 在这种情况下, 候选类必须被视为方法的声明类)
	 * @param args 方法的参数
	 * 
	 * @return 是否存在运行时匹配
	 */
	boolean matches(Method method, Class<?> targetClass, Object... args);


	/**
	 * 匹配所有方法的默认实例.
	 */
	MethodMatcher TRUE = TrueMethodMatcher.INSTANCE;

}
