package org.springframework.web.servlet.mvc.method.annotation;

import org.springframework.core.MethodParameter;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.method.support.AsyncHandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * 处理{@link org.springframework.util.concurrent.ListenableFuture}类型的返回值.
 *
 * @deprecated 从4.3开始, {@link DeferredResultMethodReturnValueHandler}通过适配器机制支持ListenableFuture返回值.
 */
@Deprecated
public class ListenableFutureReturnValueHandler implements AsyncHandlerMethodReturnValueHandler {

	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		return ListenableFuture.class.isAssignableFrom(returnType.getParameterType());
	}

	@Override
	public boolean isAsyncReturnValue(Object returnValue, MethodParameter returnType) {
		return (returnValue != null && returnValue instanceof ListenableFuture);
	}

	@Override
	public void handleReturnValue(Object returnValue, MethodParameter returnType,
			ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {

		if (returnValue == null) {
			mavContainer.setRequestHandled(true);
			return;
		}

		final DeferredResult<Object> deferredResult = new DeferredResult<Object>();
		WebAsyncUtils.getAsyncManager(webRequest).startDeferredResultProcessing(deferredResult, mavContainer);

		ListenableFuture<?> future = (ListenableFuture<?>) returnValue;
		future.addCallback(new ListenableFutureCallback<Object>() {
			@Override
			public void onSuccess(Object result) {
				deferredResult.setResult(result);
			}
			@Override
			public void onFailure(Throwable ex) {
				deferredResult.setErrorResult(ex);
			}
		});
	}

}
