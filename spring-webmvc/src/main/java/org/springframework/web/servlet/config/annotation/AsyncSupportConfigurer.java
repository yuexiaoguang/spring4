package org.springframework.web.servlet.config.annotation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.web.context.request.async.CallableProcessingInterceptor;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.context.request.async.DeferredResultProcessingInterceptor;
import org.springframework.web.context.request.async.WebAsyncTask;

/**
 * 帮助配置异步请求处理选项.
 */
public class AsyncSupportConfigurer {

	private AsyncTaskExecutor taskExecutor;

	private Long timeout;

	private final List<CallableProcessingInterceptor> callableInterceptors =
			new ArrayList<CallableProcessingInterceptor>();

	private final List<DeferredResultProcessingInterceptor> deferredResultInterceptors =
			new ArrayList<DeferredResultProcessingInterceptor>();


	/**
	 * 设置当控制器方法返回{@link Callable}时使用的默认{@link AsyncTaskExecutor}.
	 * 控制器方法可以通过返回{@link WebAsyncTask}来基于每个请求覆盖此默认值.
	 * <p>默认使用{@link SimpleAsyncTaskExecutor}实例, 强烈建议在生产中更改该默认值, 因为简单执行器不会重用线程.
	 * 
	 * @param taskExecutor the task executor instance to use by default
	 */
	public AsyncSupportConfigurer setTaskExecutor(AsyncTaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
		return this;
	}

	/**
	 * 指定异步请求处理超时之前的时间量, 以毫秒为单位.
	 * 在Servlet 3中, 超时在主请求处理线程退出后开始, 并在再次调度请求时结束, 以进一步处理同时生成的结果.
	 * <p>如果未设置此值, 则使用底层实现的默认超时, e.g. 使用Servlet 3的Tomcat默认为10秒.
	 * 
	 * @param timeout 超时值, 以毫秒为单位
	 */
	public AsyncSupportConfigurer setDefaultTimeout(long timeout) {
		this.timeout = timeout;
		return this;
	}

	/**
	 * 配置生命周期拦截器, 并在控制器返回{@link java.util.concurrent.Callable}时启动并发请求执行的回调.
	 * 
	 * @param interceptors 要注册的拦截器
	 */
	public AsyncSupportConfigurer registerCallableInterceptors(CallableProcessingInterceptor... interceptors) {
		this.callableInterceptors.addAll(Arrays.asList(interceptors));
		return this;
	}

	/**
	 * 配置生命周期拦截器, 并在控制器返回{@link DeferredResult}时启动并发请求执行的回调.
	 * 
	 * @param interceptors 要注册的拦截器
	 */
	public AsyncSupportConfigurer registerDeferredResultInterceptors(DeferredResultProcessingInterceptor... interceptors) {
		this.deferredResultInterceptors.addAll(Arrays.asList(interceptors));
		return this;
	}


	protected AsyncTaskExecutor getTaskExecutor() {
		return this.taskExecutor;
	}

	protected Long getTimeout() {
		return this.timeout;
	}

	protected List<CallableProcessingInterceptor> getCallableInterceptors() {
		return this.callableInterceptors;
	}

	protected List<DeferredResultProcessingInterceptor> getDeferredResultInterceptors() {
		return this.deferredResultInterceptors;
	}

}
