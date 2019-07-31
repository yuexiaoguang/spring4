package org.springframework.web.context.request.async;

import org.springframework.web.context.request.NativeWebRequest;

/**
 * 如果响应尚未提交, 则在超时的情况下发送503 (SERVICE_UNAVAILABLE).
 * 从4.2.8开始, 这是通过返回{@link AsyncRequestTimeoutException}作为处理结果间接完成的,
 * 然后由Spring MVC的默认异常处理作为503错误处理.
 *
 * <p>在所有其他拦截器之后注册, 因此仅在没有其他拦截器处理超时时调用.
 *
 * <p>请注意, 根据RFC 7231, 没有'Retry-After' header的503被解释为500错误, 客户端不应重试.
 * 应用程序可以安装自己的拦截器来处理超时, 并在必要时添加'Retry-After' header.
 */
public class TimeoutDeferredResultProcessingInterceptor extends DeferredResultProcessingInterceptorAdapter {

	@Override
	public <T> boolean handleTimeout(NativeWebRequest request, DeferredResult<T> result) throws Exception {
		result.setErrorResult(new AsyncRequestTimeoutException());
		return false;
	}

}
