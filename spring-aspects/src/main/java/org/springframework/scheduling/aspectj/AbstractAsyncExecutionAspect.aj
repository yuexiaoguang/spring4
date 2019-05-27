package org.springframework.scheduling.aspectj;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.aspectj.lang.annotation.SuppressAjWarnings;
import org.aspectj.lang.reflect.MethodSignature;

import org.springframework.aop.interceptor.AsyncExecutionAspectSupport;
import org.springframework.core.task.AsyncTaskExecutor;

/**
 * Abstract aspect that routes selected methods asynchronously.
 *
 * <p>This aspect needs to be injected with an implementation of a task-oriented
 * {@link java.util.concurrent.Executor} to activate it for a specific thread pool,
 * or with a {@link org.springframework.beans.factory.BeanFactory} for default
 * executor lookup. Otherwise it will simply delegate all calls synchronously.
 */
public abstract aspect AbstractAsyncExecutionAspect extends AsyncExecutionAspectSupport {

	/**
	 * Create an {@code AnnotationAsyncExecutionAspect} with a {@code null}
	 * default executor, which should instead be set via {@code #aspectOf} and
	 * {@link #setExecutor}. The same applies for {@link #setExceptionHandler}.
	 */
	public AbstractAsyncExecutionAspect() {
		super(null);
	}


	/**
	 * Apply around advice to methods matching the {@link #asyncMethod()} pointcut,
	 * submit the actual calling of the method to the correct task executor and return
	 * immediately to the caller.
	 * @return {@link Future} if the original method returns {@code Future};
	 * {@code null} otherwise
	 */
	@SuppressAjWarnings("adviceDidNotMatch")
	Object around() : asyncMethod() {
		final MethodSignature methodSignature = (MethodSignature) thisJoinPointStaticPart.getSignature();

		AsyncTaskExecutor executor = determineAsyncExecutor(methodSignature.getMethod());
		if (executor == null) {
			return proceed();
		}

		Callable<Object> task = new Callable<Object>() {
			public Object call() throws Exception {
				try {
					Object result = proceed();
					if (result instanceof Future) {
						return ((Future<?>) result).get();
					}
				}
				catch (Throwable ex) {
					handleError(ex, methodSignature.getMethod(), thisJoinPoint.getArgs());
				}
				return null;
			}};

		return doSubmit(task, executor, methodSignature.getReturnType());
	}

	/**
	 * Return the set of joinpoints at which async advice should be applied.
	 */
	public abstract pointcut asyncMethod();

}
