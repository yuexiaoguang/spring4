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
 * Handles return values of type
 * {@link org.springframework.util.concurrent.ListenableFuture}.
 *
 * @deprecated as of 4.3 {@link DeferredResultMethodReturnValueHandler} supports
 * ListenableFuture return values via an adapter mechanism.
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
