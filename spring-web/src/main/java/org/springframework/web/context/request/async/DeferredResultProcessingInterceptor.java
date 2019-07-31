package org.springframework.web.context.request.async;

import org.springframework.web.context.request.NativeWebRequest;

/**
 * 拦截并发请求处理, 通过等待从应用程序选择的线程设置{@link DeferredResult}来获得并发结果 (e.g. 响应某些外部事件).
 *
 * <p>在开始异步处理之前, 在设置{@code DeferredResult}之后以及在超时之后,
 * 或者在因任何原因(包括超时或网络错误)完成之后调用{@code DeferredResultProcessingInterceptor}.
 *
 * <p>作为一般规则, 拦截器方法引发的异常将导致异步处理通过调度回容器, 并使用Exception实例作为并发结果来恢复.
 * 然后将通过{@code HandlerExceptionResolver}机制处理此类异常.
 *
 * <p>{@link #handleTimeout(NativeWebRequest, DeferredResult) afterTimeout}方法
 * 可以设置{@code DeferredResult}以便恢复处理.
 */
public interface DeferredResultProcessingInterceptor {

	/**
	 * 在并发处理开始之前立即调用, 在启动它的同一个线程中调用.
	 * 此方法可用于在使用给定{@code DeferredResult}的并发处理开始之前捕获状态.
	 * 
	 * @param request 当前的要求
	 * @param deferredResult 当前请求的DeferredResult
	 * 
	 * @throws Exception
	 */
	<T> void beforeConcurrentHandling(NativeWebRequest request, DeferredResult<T> deferredResult) throws Exception;

	/**
	 * 在并发处理开始后立即在启动它的同一线程中调用.
	 * 此方法可用于检测具有给定{@code DeferredResult}的并发处理的开始.
	 * <p>{@code DeferredResult}可能已经设置, 例如在创建时或由另一个线程设置.
	 * 
	 * @param request 当前的要求
	 * @param deferredResult 当前请求的DeferredResult
	 * 
	 * @throws Exception
	 */
	<T> void preProcess(NativeWebRequest request, DeferredResult<T> deferredResult) throws Exception;

	/**
	 * 在通过{@link DeferredResult#setResult(Object)}或{@link DeferredResult#setErrorResult(Object)}
	 * 设置了{@code DeferredResult}之后调用, 并且还准备好处理并发结果.
	 * <p>在使用接受默认超时结果的构造函数创建{@code DeferredResult}时, 超时后也可以调用此方法.
	 * 
	 * @param request 当前的要求
	 * @param deferredResult 当前请求的DeferredResult
	 * @param concurrentResult {@code DeferredResult}的结果
	 * 
	 * @throws Exception
	 */
	<T> void postProcess(NativeWebRequest request, DeferredResult<T> deferredResult, Object concurrentResult) throws Exception;

	/**
	 * 当异步请求在设置的{@code DeferredResult}之前超时时, 从容器线程调用.
	 * 实现可以调用{@link DeferredResult#setResult(Object) setResult}
	 * 或{@link DeferredResult#setErrorResult(Object) setErrorResult}来恢复处理.
	 * 
	 * @param request 当前的要求
	 * @param deferredResult 当前请求的DeferredResult;
	 * 如果设置了{@code DeferredResult}, 则恢复并发处理并且不调用后续的拦截器
	 * 
	 * @return {@code true} 如果处理应该继续, 或{@code false} 如果不应该调用其他拦截器
	 * @throws Exception
	 */
	<T> boolean handleTimeout(NativeWebRequest request, DeferredResult<T> deferredResult) throws Exception;

	/**
	 * 当异步请求因任何原因(包括超时和网络错误)完成时, 从容器线程调用.
	 * 此方法对于检测{@code DeferredResult}实例不再可用非常有用.
	 * 
	 * @param request 当前的要求
	 * @param deferredResult 当前请求的DeferredResult
	 * 
	 * @throws Exception
	 */
	<T> void afterCompletion(NativeWebRequest request, DeferredResult<T> deferredResult) throws Exception;

}
