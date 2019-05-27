package org.springframework.core;

/**
 * 由可以返回有关当前调用堆栈的信息的对象实现的接口.
 * 在AOP中有用 (如在AspectJ cflow概念中), 但不是AOP特定的.
 *
 * @deprecated as of Spring Framework 4.3.6
 */
@Deprecated
public interface ControlFlow {

	/**
	 * 根据当前的堆栈跟踪, 检测我们是否在给定的类下.
	 * 
	 * @param clazz 要查找的clazz
	 */
	boolean under(Class<?> clazz);

	/**
	 * 根据当前堆栈跟踪, 检测我们是否在给定的类和方法下.
	 * 
	 * @param clazz 要查找的clazz
	 * @param methodName 要查找的方法的名称
	 */
	boolean under(Class<?> clazz, String methodName);

	/**
	 * 检测当前堆栈跟踪是否包含给定token.
	 * 
	 * @param token 要查找的token
	 */
	boolean underToken(String token);

}
