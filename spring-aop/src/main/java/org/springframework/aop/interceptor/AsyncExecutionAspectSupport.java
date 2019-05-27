package org.springframework.aop.interceptor;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.lang.UsesJava8;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * 异步方法执行切面的基类, 例如
 * {@code org.springframework.scheduling.annotation.AnnotationAsyncExecutionInterceptor}
 * 或 {@code org.springframework.scheduling.aspectj.AnnotationAsyncExecutionAspect}.
 *
 * <p>提供基于方法的特定执行器支持.
 * 必须使用默认的{@code Executor}构造{@code AsyncExecutionAspectSupport}对象,
 * 但是每个单独的方法可以进一步限定在执行它时使用的特定{@code Executor} bean, e.g. 通过一个注解属性.
 */
public abstract class AsyncExecutionAspectSupport implements BeanFactoryAware {

	/**
	 * 要获取的{@link TaskExecutor} bean的默认名称: "taskExecutor".
	 * <p>请注意，初始查找按类型进行; 这只是在上下文中找到多个执行器bean的情况下的后备.
	 * @since 4.2.6
	 */
	public static final String DEFAULT_TASK_EXECUTOR_BEAN_NAME = "taskExecutor";


	// Java 8's CompletableFuture type present?
	private static final boolean completableFuturePresent = ClassUtils.isPresent(
			"java.util.concurrent.CompletableFuture", AsyncExecutionInterceptor.class.getClassLoader());


	protected final Log logger = LogFactory.getLog(getClass());

	private final Map<Method, AsyncTaskExecutor> executors = new ConcurrentHashMap<Method, AsyncTaskExecutor>(16);

	private volatile Executor defaultExecutor;

	private AsyncUncaughtExceptionHandler exceptionHandler;

	private BeanFactory beanFactory;


	/**
	 * @param defaultExecutor 要委托的{@code Executor} (通常是 Spring {@code AsyncTaskExecutor}
	 * 或 {@link java.util.concurrent.ExecutorService}), 除非通过异步方法上的限定符请求了更具体的执行器,
	 * 在这种情况下, 执行程序将在调用时查找封闭的bean工厂
	 */
	public AsyncExecutionAspectSupport(Executor defaultExecutor) {
		this(defaultExecutor, new SimpleAsyncUncaughtExceptionHandler());
	}

	/**
	 * @param defaultExecutor 要委托的{@code Executor} (通常是 Spring {@code AsyncTaskExecutor}
	 * 或 {@link java.util.concurrent.ExecutorService}), 除非通过异步方法上的限定符请求了更具体的执行器,
	 * 在这种情况下, 执行程序将在调用时查找封闭的bean工厂
	 * @param exceptionHandler 要使用的{@link AsyncUncaughtExceptionHandler}
	 */
	public AsyncExecutionAspectSupport(Executor defaultExecutor, AsyncUncaughtExceptionHandler exceptionHandler) {
		this.defaultExecutor = defaultExecutor;
		this.exceptionHandler = exceptionHandler;
	}


	/**
	 * 提供执行异步方法时要使用的执行器.
	 * 
	 * @param defaultExecutor 要委托的{@code Executor} (通常是 Spring {@code AsyncTaskExecutor}
	 * 或 {@link java.util.concurrent.ExecutorService}), 除非通过异步方法上的限定符请求了更具体的执行器,
	 * 在这种情况下, 执行程序将在调用时查找封闭的bean工厂
	 */
	public void setExecutor(Executor defaultExecutor) {
		this.defaultExecutor = defaultExecutor;
	}

	/**
	 * 提供{@link AsyncUncaughtExceptionHandler}, 用于处理通过{@code void}返回类型调用异步方法引发的异常.
	 */
	public void setExceptionHandler(AsyncUncaughtExceptionHandler exceptionHandler) {
		this.exceptionHandler = exceptionHandler;
	}

	/**
	 * 设置通过限定符查找执行器或依赖于默认执行器查找算法时使用的{@link BeanFactory}.
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}


	/**
	 * 确定执行给定方法时要使用的特定执行器.
	 * 最好应该返回{@link AsyncListenableTaskExecutor}实现.
	 * 
	 * @return 要使用的执行器 (或{@code null}, 如果没有可用的默认执行器)
	 */
	protected AsyncTaskExecutor determineAsyncExecutor(Method method) {
		AsyncTaskExecutor executor = this.executors.get(method);
		if (executor == null) {
			Executor targetExecutor;
			String qualifier = getExecutorQualifier(method);
			if (StringUtils.hasLength(qualifier)) {
				targetExecutor = findQualifiedExecutor(this.beanFactory, qualifier);
			}
			else {
				targetExecutor = this.defaultExecutor;
				if (targetExecutor == null) {
					synchronized (this.executors) {
						if (this.defaultExecutor == null) {
							this.defaultExecutor = getDefaultExecutor(this.beanFactory);
						}
						targetExecutor = this.defaultExecutor;
					}
				}
			}
			if (targetExecutor == null) {
				return null;
			}
			executor = (targetExecutor instanceof AsyncListenableTaskExecutor ?
					(AsyncListenableTaskExecutor) targetExecutor : new TaskExecutorAdapter(targetExecutor));
			this.executors.put(method, executor);
		}
		return executor;
	}

	/**
	 * 返回执行给定异步方法时要使用的执行器的限定符或bean名称, 通常以注解属性的形式指定.
	 * 返回空字符串或{@code null}表示没有指定特定的执行程序, 并且应该使用{@linkplain #setExecutor(Executor) default executor}.
	 * 
	 * @param method 检查执行器限定符元数据的方法
	 * 
	 * @return 限定符, 或空字符串或 {@code null}
	 */
	protected abstract String getExecutorQualifier(Method method);

	/**
	 * 检索给定限定符的目标执行器.
	 * 
	 * @param qualifier 要解析的限定符
	 * 
	 * @return 目标执行器, 或{@code null}
	 * @since 4.2.6
	 */
	protected Executor findQualifiedExecutor(BeanFactory beanFactory, String qualifier) {
		if (beanFactory == null) {
			throw new IllegalStateException("BeanFactory must be set on " + getClass().getSimpleName() +
					" to access qualified executor '" + qualifier + "'");
		}
		return BeanFactoryAnnotationUtils.qualifiedBeanOfType(beanFactory, Executor.class, qualifier);
	}

	/**
	 * 检索或构建此增强实例的默认执行器. 从此处返回的执行器将被缓存以供进一步使用.
	 * <p>默认实现在上下文中搜索唯一的{@link TaskExecutor} bean, 或者用于名为"taskExecutor"的{@link Executor} bean.
	 * 如果两者都不可解析, 此实现将返回 {@code null}.
	 * 
	 * @param beanFactory 用于默认执行器查找的BeanFactory
	 * 
	 * @return 默认执行器, 或 {@code null}
	 * @since 4.2.6
	 */
	protected Executor getDefaultExecutor(BeanFactory beanFactory) {
		if (beanFactory != null) {
			try {
				// 搜索TaskExecutor bean... 不是普通的Executor，因为它也会与ScheduledExecutorService匹配，这对于我们这里的目的是不可用的
				// TaskExecutor更清晰地为它设计.
				return beanFactory.getBean(TaskExecutor.class);
			}
			catch (NoUniqueBeanDefinitionException ex) {
				logger.debug("Could not find unique TaskExecutor bean", ex);
				try {
					return beanFactory.getBean(DEFAULT_TASK_EXECUTOR_BEAN_NAME, Executor.class);
				}
				catch (NoSuchBeanDefinitionException ex2) {
					if (logger.isInfoEnabled()) {
						logger.info("More than one TaskExecutor bean found within the context, and none is named " +
								"'taskExecutor'. Mark one of them as primary or name it 'taskExecutor' (possibly " +
								"as an alias) in order to use it for async processing: " + ex.getBeanNamesFound());
					}
				}
			}
			catch (NoSuchBeanDefinitionException ex) {
				logger.debug("Could not find default TaskExecutor bean", ex);
				try {
					return beanFactory.getBean(DEFAULT_TASK_EXECUTOR_BEAN_NAME, Executor.class);
				}
				catch (NoSuchBeanDefinitionException ex2) {
					logger.info("No task executor bean found for async processing: " +
							"no bean of type TaskExecutor and no bean named 'taskExecutor' either");
				}
				// Giving up -> 要么使用本地默认执行器，要么根本不使用...
			}
		}
		return null;
	}


	/**
	 * 使用选择的执行器执行给定的任务.
	 * 
	 * @param task 要执行的任务
	 * @param executor 选择的执行器
	 * @param returnType 声明的返回类型 (可能是{@link Future}变体)
	 * 
	 * @return 执行结果 (可能是相应的{@link Future}句柄)
	 */
	protected Object doSubmit(Callable<Object> task, AsyncTaskExecutor executor, Class<?> returnType) {
		if (completableFuturePresent) {
			Future<Object> result = CompletableFutureDelegate.processCompletableFuture(returnType, task, executor);
			if (result != null) {
				return result;
			}
		}
		if (ListenableFuture.class.isAssignableFrom(returnType)) {
			return ((AsyncListenableTaskExecutor) executor).submitListenable(task);
		}
		else if (Future.class.isAssignableFrom(returnType)) {
			return executor.submit(task);
		}
		else {
			executor.submit(task);
			return null;
		}
	}

	/**
	 * 处理异步调用指定的{@link Method}时抛出的致命错误.
	 * <p>如果方法的返回类型是{@link Future}对象, 原始异常可以通过将其抛出到更高级别来传播.
	 * 但是, 对于所有其他情况, 异常不会传回客户端. 在后一种情况下, 当前的{@link AsyncUncaughtExceptionHandler}将用于管理此类异常.
	 * 
	 * @param ex 要处理的异常
	 * @param method 被调用的方法
	 * @param params 用于调用方法的参数
	 */
	protected void handleError(Throwable ex, Method method, Object... params) throws Exception {
		if (Future.class.isAssignableFrom(method.getReturnType())) {
			ReflectionUtils.rethrowException(ex);
		}
		else {
			// 无法使用默认执行器将异常传输给调用者
			try {
				this.exceptionHandler.handleUncaughtException(ex, method, params);
			}
			catch (Throwable ex2) {
				logger.error("Exception handler for async method '" + method.toGenericString() +
						"' threw unexpected exception itself", ex2);
			}
		}
	}


	/**
	 * 内部类, 以避免对Java 8的硬依赖.
	 */
	@UsesJava8
	private static class CompletableFutureDelegate {

		public static <T> Future<T> processCompletableFuture(Class<?> returnType, final Callable<T> task, Executor executor) {
			if (!CompletableFuture.class.isAssignableFrom(returnType)) {
				return null;
			}
			return CompletableFuture.supplyAsync(new Supplier<T>() {
				@Override
				public T get() {
					try {
						return task.call();
					}
					catch (Throwable ex) {
						throw new CompletionException(ex);
					}
				}
			}, executor);
		}
	}
}
