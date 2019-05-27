package org.springframework.aop.interceptor;

import java.lang.reflect.Method;

/**
 * 处理从异步方法抛出的未捕获异常的策略.
 *
 * <p>异步方法通常返回一个{@link java.util.concurrent.Future}实例, 该实例提供对底层异常的访问.
 * 当方法没有提供返回类型时, 此处理器可用于管理未捕获的异常.
 */
public interface AsyncUncaughtExceptionHandler {

	/**
	 * 处理从异步方法抛出的给定未捕获异常.
	 * 
	 * @param ex 从异步方法抛出的异常
	 * @param method 异步方法
	 * @param params 用于调用方法的参数
	 */
	void handleUncaughtException(Throwable ex, Method method, Object... params);

}
