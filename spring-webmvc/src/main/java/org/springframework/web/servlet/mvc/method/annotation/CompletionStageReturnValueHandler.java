package org.springframework.web.servlet.mvc.method.annotation;

import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;

import org.springframework.core.MethodParameter;
import org.springframework.lang.UsesJava8;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.method.support.AsyncHandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Handles return values of type {@link CompletionStage} (implemented by
 * {@link java.util.concurrent.CompletableFuture} for example).
 *
 * @deprecated as of 4.3 {@link DeferredResultMethodReturnValueHandler} supports
 * CompletionStage return values via an adapter mechanism.
 */
@Deprecated
@UsesJava8
public class CompletionStageReturnValueHandler implements AsyncHandlerMethodReturnValueHandler {

	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		return CompletionStage.class.isAssignableFrom(returnType.getParameterType());
	}

	@Override
	public boolean isAsyncReturnValue(Object returnValue, MethodParameter returnType) {
		return (returnValue != null && returnValue instanceof CompletionStage);
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

		@SuppressWarnings("unchecked")
		CompletionStage<Object> future = (CompletionStage<Object>) returnValue;
		future.thenAccept(new Consumer<Object>() {
			@Override
			public void accept(Object result) {
				deferredResult.setResult(result);
			}
		});
		future.exceptionally(new Function<Throwable, Object>() {
			@Override
			public Object apply(Throwable ex) {
				deferredResult.setErrorResult(ex);
				return null;
			}
		});
	}

}
