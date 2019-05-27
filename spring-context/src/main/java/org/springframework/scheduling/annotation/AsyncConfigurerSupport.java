package org.springframework.scheduling.annotation;

import java.util.concurrent.Executor;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

/**
 * 实现所有方法的{@link AsyncConfigurer}, 以便使用默认值.
 * 提供直接实现{@link AsyncConfigurer}的向后兼容替代方案.
 */
public class AsyncConfigurerSupport implements AsyncConfigurer {

	@Override
	public Executor getAsyncExecutor() {
		return null;
	}

	@Override
	public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
		return null;
	}

}
