package org.springframework.web.context.request.async;

import java.util.concurrent.Callable;

import org.springframework.web.context.request.NativeWebRequest;

/**
 * {@link CallableProcessingInterceptor}接口的抽象适配器类, 用于简化单个方法的实现.
 */
public abstract class CallableProcessingInterceptorAdapter implements CallableProcessingInterceptor {

	/**
	 * This implementation is empty.
	 */
	@Override
	public <T> void beforeConcurrentHandling(NativeWebRequest request, Callable<T> task) throws Exception {
	}

	/**
	 * This implementation is empty.
	 */
	@Override
	public <T> void preProcess(NativeWebRequest request, Callable<T> task) throws Exception {
	}

	/**
	 * This implementation is empty.
	 */
	@Override
	public <T> void postProcess(NativeWebRequest request, Callable<T> task, Object concurrentResult) throws Exception {
	}

	/**
	 * 此实现始终返回{@link CallableProcessingInterceptor#RESULT_NONE RESULT_NONE}.
	 */
	@Override
	public <T> Object handleTimeout(NativeWebRequest request, Callable<T> task) throws Exception {
		return RESULT_NONE;
	}

	/**
	 * This implementation is empty.
	 */
	@Override
	public <T> void afterCompletion(NativeWebRequest request, Callable<T> task) throws Exception {
	}

}
