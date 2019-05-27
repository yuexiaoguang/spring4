package org.springframework.messaging.handler.invocation;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.springframework.core.MethodParameter;
import org.springframework.lang.UsesJava8;
import org.springframework.util.concurrent.CompletableToListenableFutureAdapter;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * 支持{@link CompletableFuture} (和4.3.7以及{@link CompletionStage}) 作为返回值类型.
 */
@UsesJava8
public class CompletableFutureReturnValueHandler extends AbstractAsyncReturnValueHandler {

	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		return CompletionStage.class.isAssignableFrom(returnType.getParameterType());
	}

	@Override
	@SuppressWarnings("unchecked")
	public ListenableFuture<?> toListenableFuture(Object returnValue, MethodParameter returnType) {
		return new CompletableToListenableFutureAdapter<Object>((CompletionStage<Object>) returnValue);
	}

}
