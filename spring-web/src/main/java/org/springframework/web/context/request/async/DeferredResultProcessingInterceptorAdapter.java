package org.springframework.web.context.request.async;

import org.springframework.web.context.request.NativeWebRequest;

/**
 * {@link DeferredResultProcessingInterceptor}接口的抽象适配器类, 用于简化单个方法实现.
 */
public abstract class DeferredResultProcessingInterceptorAdapter implements DeferredResultProcessingInterceptor {

	/**
	 * This implementation is empty.
	 */
	@Override
	public <T> void beforeConcurrentHandling(NativeWebRequest request, DeferredResult<T> deferredResult)
			throws Exception {
	}

	/**
	 * This implementation is empty.
	 */
	@Override
	public <T> void preProcess(NativeWebRequest request, DeferredResult<T> deferredResult) throws Exception {
	}

	/**
	 * This implementation is empty.
	 */
	@Override
	public <T> void postProcess(NativeWebRequest request, DeferredResult<T> deferredResult,
			Object concurrentResult) throws Exception {
	}

	/**
	 * 此实现默认返回{@code true}, 允许其他拦截器处理超时.
	 */
	@Override
	public <T> boolean handleTimeout(NativeWebRequest request, DeferredResult<T> deferredResult) throws Exception {
		return true;
	}

	/**
	 * This implementation is empty.
	 */
	@Override
	public <T> void afterCompletion(NativeWebRequest request, DeferredResult<T> deferredResult) throws Exception {
	}

}
