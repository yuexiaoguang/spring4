package org.springframework.web.context.request.async;

import java.util.concurrent.Callable;

import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * 拦截并发请求处理, 其中通过使用{@link AsyncTaskExecutor}代表应用程序执行{@link Callable}来获取并发结果.
 *
 * <p>在调用异步线程中的{@code Callable}任务之前和之后, 以及从容器线程调用超时,
 * 或者在因任何原因(包括超时或网络错误)完成之后调用{@code CallableProcessingInterceptor}.
 *
 * <p>作为一般规则, 拦截器方法引发的异常将导致异步处理通过调度回容器并使用Exception实例作为并发结果来恢复.
 * 然后将通过{@code HandlerExceptionResolver}机制处理此类异常.
 *
 * <p>{@link #handleTimeout(NativeWebRequest, Callable) handleTimeout}方法可以选择用于恢复处理的值.
 */
public interface CallableProcessingInterceptor {

	/**
	 * 常量, 表示此拦截器未确定任何结果, 为后续拦截器提供了机会.
	 */
	Object RESULT_NONE = new Object();

	/**
	 * 常量, 指示此拦截器已处理响应而没有结果, 并且不会调用其他拦截器.
	 */
	Object RESPONSE_HANDLED = new Object();


	/**
	 * 在提交{@code Callable}以进行并发处理的原始线程中的并发处理开始之前调用.
	 * <p>这对于在调用{@link Callable}之前捕获当前线程的状态很有用.
	 * 捕获状态后, 可以将其转移到{@link #preProcess(NativeWebRequest, Callable)}中的新{@link Thread}.
	 * 捕获Spring Security的SecurityContextHolder的状态并将其迁移到新的Thread是一个具体的例子, 说明这是有用的.
	 * 
	 * @param request 当前的请求
	 * @param task 当前异步请求的任务
	 * 
	 * @throws Exception
	 */
	<T> void  beforeConcurrentHandling(NativeWebRequest request, Callable<T> task) throws Exception;

	/**
	 * 在执行{@code Callable}的异步线程中启动并发处理之后以及{@code Callable}的实际调用之前调用.
	 * 
	 * @param request 当前的请求
	 * @param task 当前异步请求的任务
	 * 
	 * @throws Exception
	 */
	<T> void preProcess(NativeWebRequest request, Callable<T> task) throws Exception;

	/**
	 * 在执行{@code Callable}的异步线程中{@code Callable}生成结果后调用.
	 * 这个方法可以在{@code afterTimeout}或{@code afterCompletion}之后调用, 具体取决于{@code Callable}何时完成处理.
	 * 
	 * @param request 当前的请求
	 * @param task 当前异步请求的任务
	 * @param concurrentResult 并发处理的结果, 如果{@code Callable}引发异常, 则可以是{@link Throwable}
	 * 
	 * @throws Exception
	 */
	<T> void postProcess(NativeWebRequest request, Callable<T> task, Object concurrentResult) throws Exception;

	/**
	 * 在{@code Callable}任务完成之前异步请求超时时, 从容器线程调用.
	 * 实现可能返回一个值, 包括{@link Exception}, 而不是使用{@link Callable}没有及时返回的值.
	 * 
	 * @param request 当前的请求
	 * @param task 当前异步请求的任务
	 * 
	 * @return 并发结果值; 如果值不是{@link #RESULT_NONE}或{@link #RESPONSE_HANDLED},
	 * 则恢复并发处理并且不调用后续的拦截器
	 * @throws Exception
	 */
	<T> Object handleTimeout(NativeWebRequest request, Callable<T> task) throws Exception;

	/**
	 * 异步处理因任何原因(包括超时或网络错误)完成时从容器线程调用.
	 * 
	 * @param request 当前的请求
	 * @param task 当前异步请求的任务
	 * 
	 * @throws Exception
	 */
	<T> void afterCompletion(NativeWebRequest request, Callable<T> task) throws Exception;

}
