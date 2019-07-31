package org.springframework.web.context.request.async;

import java.util.concurrent.Callable;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.util.Assert;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * {@link Callable}的保存器, 超时值和任务执行器.
 */
public class WebAsyncTask<V> implements BeanFactoryAware {

	private final Callable<V> callable;

	private Long timeout;

	private AsyncTaskExecutor executor;

	private String executorName;

	private BeanFactory beanFactory;

	private Callable<V> timeoutCallback;

	private Runnable completionCallback;


	/**
	 * @param callable 并发处理的调用
	 */
	public WebAsyncTask(Callable<V> callable) {
		Assert.notNull(callable, "Callable must not be null");
		this.callable = callable;
	}

	/**
	 * @param timeout 超时值, 以毫秒为单位
	 * @param callable 并发处理的调用
	 */
	public WebAsyncTask(long timeout, Callable<V> callable) {
		this(callable);
		this.timeout = timeout;
	}

	/**
	 * @param timeout 超时值, 以毫秒为单位; 如果为{@code null}则忽略
	 * @param executorName 要使用的执行器bean的名称
	 * @param callable 并发处理的调用
	 */
	public WebAsyncTask(Long timeout, String executorName, Callable<V> callable) {
		this(callable);
		Assert.notNull(executorName, "Executor name must not be null");
		this.executorName = executorName;
		this.timeout = timeout;
	}

	/**
	 * @param timeout 超时值, 以毫秒为单位; 如果为{@code null}则忽略
	 * @param executor 要使用的执行器
	 * @param callable 并发处理的调用
	 */
	public WebAsyncTask(Long timeout, AsyncTaskExecutor executor, Callable<V> callable) {
		this(callable);
		Assert.notNull(executor, "Executor must not be null");
		this.executor = executor;
		this.timeout = timeout;
	}


	/**
	 * 返回用于并发处理的{@link Callable} (never {@code null}).
	 */
	public Callable<?> getCallable() {
		return this.callable;
	}

	/**
	 * 返回超时值, 以毫秒为单位, 如果未设置超时, 则返回{@code null}.
	 */
	public Long getTimeout() {
		return this.timeout;
	}

	/**
	 * 用于解析执行器名称的{@link BeanFactory}.
	 * <p>在Spring MVC控制器中使用{@code WebAsyncTask}时, 将自动设置此工厂引用.
	 */
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	/**
	 * 返回用于并发处理AsyncTaskExecutor, 如果没有指定, 则返回{@code null}.
	 */
	public AsyncTaskExecutor getExecutor() {
		if (this.executor != null) {
			return this.executor;
		}
		else if (this.executorName != null) {
			Assert.state(this.beanFactory != null, "BeanFactory is required to look up an executor bean by name");
			return this.beanFactory.getBean(this.executorName, AsyncTaskExecutor.class);
		}
		else {
			return null;
		}
	}


	/**
	 * 注册在异步请求超时时调用的代码.
	 * <p>当{@code Callable}完成之前异步请求超时时, 将从容器线程调用此方法.
	 * 回调在同一个线程中执行, 因此应该无阻塞地返回.
	 * 它可能返回一个替代值, 包括{@link Exception}或返回
	 * {@link CallableProcessingInterceptor#RESULT_NONE RESULT_NONE}.
	 */
	public void onTimeout(Callable<V> callback) {
		this.timeoutCallback = callback;
	}

	/**
	 * 注册在异步请求完成时调用的代码.
	 * <p>当异步请求因任何原因(包括超时和网络错误)完成时, 将从容器线程调用此方法.
	 */
	public void onCompletion(Runnable callback) {
		this.completionCallback = callback;
	}

	CallableProcessingInterceptor getInterceptor() {
		return new CallableProcessingInterceptorAdapter() {
			@Override
			public <T> Object handleTimeout(NativeWebRequest request, Callable<T> task) throws Exception {
				return (timeoutCallback != null ? timeoutCallback.call() : CallableProcessingInterceptor.RESULT_NONE);
			}
			@Override
			public <T> void afterCompletion(NativeWebRequest request, Callable<T> task) throws Exception {
				if (completionCallback != null) {
					completionCallback.run();
				}
			}
		};
	}

}
