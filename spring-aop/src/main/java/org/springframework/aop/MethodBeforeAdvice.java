package org.springframework.aop;

import java.lang.reflect.Method;

/**
 * 在调用方法之前调用的增强. 这样的增强无法阻止方法调用的进行, 除非它们抛出一个 Throwable.
 */
public interface MethodBeforeAdvice extends BeforeAdvice {

	/**
	 * 调用给定方法之前的回调.
	 * 
	 * @param method 被调用的方法
	 * @param args 方法的参数
	 * @param target 方法调用的目标. 可能是 {@code null}.
	 * 
	 * @throws Throwable 如果此对象希望中止调用. 如果方法签名允许，则抛出的任何异常都将返回给调用者. 否则，异常将被包装为运行时异常.
	 */
	void before(Method method, Object[] args, Object target) throws Throwable;

}
