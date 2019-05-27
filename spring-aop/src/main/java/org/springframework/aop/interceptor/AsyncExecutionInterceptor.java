package org.springframework.aop.interceptor;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.Ordered;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.util.ClassUtils;

/**
 * AOP联盟{@code MethodInterceptor}异步处理方法调用, 使用给定的{@link org.springframework.core.task.AsyncTaskExecutor}.
 * 通常与{@link org.springframework.scheduling.annotation.Async}注解一起使用.
 *
 * <p>就目标方法签名而言, 支持任何参数类型. 但是, 返回类型被约束为{@code void}或{@code java.util.concurrent.Future}.
 * 在后一种情况下, 从代理返回的Future句柄将是一个实际的异步Future, 可用于跟踪异步方法执行的结果.
 * 但是, 因为目标方法需要实现相同的签名, 它必须返回一个临时的Future句柄, 它只传递返回值
 * (就像Spring的{@link org.springframework.scheduling.annotation.AsyncResult}或EJB 3.1的{@code javax.ejb.AsyncResult}).
 *
 * <p>返回类型为{@code java.util.concurrent.Future}时, 执行期间抛出的任何异常都可以由调用者访问和管理.
 * 但是使用{@code void}返回类型, 此类异常无法传回. 在这种情况下, 可以注册{@link AsyncUncaughtExceptionHandler}来处理此类异常.
 *
 * <p>从Spring 3.1.2开始, {@code AnnotationAsyncExecutionInterceptor}子类是首选使用的, 
 * 因为它支持与Spring的{@code @Async}注解一起的执行器资格认证.
 */
public class AsyncExecutionInterceptor extends AsyncExecutionAspectSupport implements MethodInterceptor, Ordered {

	/**
	 * @param defaultExecutor 要委托的{@link Executor} (通常是Spring {@link AsyncTaskExecutor}或{@link java.util.concurrent.ExecutorService});
	 * 截至4.2.6, 否则将构建此拦截器的本地执行器
	 */
	public AsyncExecutionInterceptor(Executor defaultExecutor) {
		super(defaultExecutor);
	}

	/**
	 * @param defaultExecutor 要委托的{@link Executor} (通常是Spring {@link AsyncTaskExecutor}或{@link java.util.concurrent.ExecutorService});
	 * 截至4.2.6, 否则将构建此拦截器的本地执行器
	 * @param exceptionHandler 要使用的{@link AsyncUncaughtExceptionHandler}
	 */
	public AsyncExecutionInterceptor(Executor defaultExecutor, AsyncUncaughtExceptionHandler exceptionHandler) {
		super(defaultExecutor, exceptionHandler);
	}


	/**
	 * 拦截给定的方法调用, 将方法的实际调用提交给正确的任务执行器, 并立即返回给调用者.
	 * 
	 * @param invocation 要拦截和异步的方法
	 * 
	 * @return {@link Future} 如果原始方法返回{@code Future}; 否则{@code null}.
	 */
	@Override
	public Object invoke(final MethodInvocation invocation) throws Throwable {
		Class<?> targetClass = (invocation.getThis() != null ? AopUtils.getTargetClass(invocation.getThis()) : null);
		Method specificMethod = ClassUtils.getMostSpecificMethod(invocation.getMethod(), targetClass);
		final Method userDeclaredMethod = BridgeMethodResolver.findBridgedMethod(specificMethod);

		AsyncTaskExecutor executor = determineAsyncExecutor(userDeclaredMethod);
		if (executor == null) {
			throw new IllegalStateException(
					"No executor specified and no default executor set on AsyncExecutionInterceptor either");
		}

		Callable<Object> task = new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				try {
					Object result = invocation.proceed();
					if (result instanceof Future) {
						return ((Future<?>) result).get();
					}
				}
				catch (ExecutionException ex) {
					handleError(ex.getCause(), userDeclaredMethod, invocation.getArguments());
				}
				catch (Throwable ex) {
					handleError(ex, userDeclaredMethod, invocation.getArguments());
				}
				return null;
			}
		};

		return doSubmit(task, executor, invocation.getMethod().getReturnType());
	}

	/**
	 * 这个实现为了Spring 3.1.2的兼容性, 无操作.
	 * 子类可以重写以提供对提取限定符信息的支持, 即通过给定方法的注解.
	 * 
	 * @return always {@code null}
	 * @since 3.1.2
	 */
	@Override
	protected String getExecutorQualifier(Method method) {
		return null;
	}

	/**
	 * 此实现在上下文中搜索唯一的{@link org.springframework.core.task.TaskExecutor} bean,
	 * 或者用于名为“taskExecutor”的{@link Executor} bean.
	 * 如果两者都不可解析 (e.g. 如果没有配置{@code BeanFactory}),
	 * 如果没有找到默认值, 此实现将回退到新创建的{@link SimpleAsyncTaskExecutor}实例以供本地使用.
	 */
	@Override
	protected Executor getDefaultExecutor(BeanFactory beanFactory) {
		Executor defaultExecutor = super.getDefaultExecutor(beanFactory);
		return (defaultExecutor != null ? defaultExecutor : new SimpleAsyncTaskExecutor());
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}
}
