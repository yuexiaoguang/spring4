package org.springframework.scheduling.annotation;

import java.util.concurrent.Executor;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

/**
 * 处理异步方法调用时, 或处理用于处理{@code void}返回类型的异步方法抛出的异常的{@link AsyncUncaughtExceptionHandler}实例时,
 * 带@{@link EnableAsync}注解的@{@link org.springframework.context.annotation.Configuration Configuration}类实现的接口,
 * 希望自定义使用的{@link Executor}实例.
 *
 * <p>如果只需要自定义一个元素, 请考虑使用{@link AsyncConfigurerSupport}为这两种方法提供默认实现.
 * 此外, 如果将来引入新的自定义选项, 将保证此接口的向后兼容性.
 *
 * <p>See @{@link EnableAsync} for usage examples.
 */
public interface AsyncConfigurer {

	/**
	 * 处理异步方法调用时要使用的{@link Executor}实例.
	 */
	Executor getAsyncExecutor();

	/**
	 * 在{@code void}返回类型的异步方法执行期间抛出异常时, 要使用的{@link AsyncUncaughtExceptionHandler}实例.
	 */
	AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler();

}
