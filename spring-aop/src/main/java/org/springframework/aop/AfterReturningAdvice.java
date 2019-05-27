package org.springframework.aop;

import java.lang.reflect.Method;

/**
 * 返回增强, 仅在正常方法返回时调用, 而不是抛出异常. 这样的增强可以看到返回值, 但不能修改它.
 */
public interface AfterReturningAdvice extends AfterAdvice {

	/**
	 * 成功返回给定方法后的回调.
	 * 
	 * @param returnValue 方法返回的值
	 * @param method 被调用的方法
	 * @param args 方法的参数
	 * @param target 方法调用的目标. 可能是{@code null}.
	 * 
	 * @throws Throwable 如果此对象希望中止调用. 如果方法签名允许，则抛出的任何异常都将返回给调用者. 否则，异常将被包装为运行时异常.
	 */
	void afterReturning(Object returnValue, Method method, Object[] args, Object target) throws Throwable;

}
