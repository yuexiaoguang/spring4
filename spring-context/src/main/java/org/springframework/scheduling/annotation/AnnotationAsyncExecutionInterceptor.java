package org.springframework.scheduling.annotation;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

import org.springframework.aop.interceptor.AsyncExecutionInterceptor;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.core.annotation.AnnotatedElementUtils;

/**
 * {@link AsyncExecutionInterceptor}的专业化, 它根据{@link Async}注解将方法执行委托给{@code Executor}.
 * 专门用于支持Spring 3.1.2中引入的{@link Async#value()}执行器限定机制.
 * 支持在方法中通过{@code @Async}检测限定符元数据或声明类级别.
 * See {@link #getExecutorQualifier(Method)} for details.
 */
public class AnnotationAsyncExecutionInterceptor extends AsyncExecutionInterceptor {

	/**
	 * 给定的执行器和一个简单的{@link AsyncUncaughtExceptionHandler}.
	 * 
	 * @param defaultExecutor 默认情况下使用的执行器, 如果使用{@link Async#value()}在方法级别没有更多特定的执行器被限定;
	 * 从4.2.6开始, 将构建此拦截器的本地执行器
	 */
	public AnnotationAsyncExecutionInterceptor(Executor defaultExecutor) {
		super(defaultExecutor);
	}

	/**
	 * @param defaultExecutor 默认情况下使用的执行器, 如果使用{@link Async#value()}在方法级别没有更多特定的执行器被限定;
	 * 从4.2.6开始, 将构建此拦截器的本地执行器
	 * @param exceptionHandler 用于处理由{@code void}返回类型的异步方法执行引发的异常的{@link AsyncUncaughtExceptionHandler}
	 */
	public AnnotationAsyncExecutionInterceptor(Executor defaultExecutor, AsyncUncaughtExceptionHandler exceptionHandler) {
		super(defaultExecutor, exceptionHandler);
	}


	/**
	 * 返回执行给定方法时要使用的执行器的限定符或bean名称, 在方法或声明类级别通过{@link Async#value}指定.
	 * 如果在方法和类级别都指定了{@code @Async}, 则方法的{@code #value}优先 (即使是空字符串, 也表示应该优先使用默认执行器).
	 * 
	 * @param method 内省执行器限定符元数据的方法
	 * 
	 * @return 限定符; 否则为空字符串, 表示应使用{@linkplain #setExecutor(Executor) 默认执行器}
	 */
	@Override
	protected String getExecutorQualifier(Method method) {
		// Maintainer's note: 此处所做的更改也应在AnnotationAsyncExecutionAspect#getExecutorQualifier中进行
		Async async = AnnotatedElementUtils.findMergedAnnotation(method, Async.class);
		if (async == null) {
			async = AnnotatedElementUtils.findMergedAnnotation(method.getDeclaringClass(), Async.class);
		}
		return (async != null ? async.value() : null);
	}
}
