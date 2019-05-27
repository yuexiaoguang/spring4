package org.springframework.messaging.handler.invocation;

import org.springframework.core.MethodParameter;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * 支持{@link ListenableFuture}作为返回值类型.
 */
public class ListenableFutureReturnValueHandler extends AbstractAsyncReturnValueHandler {

	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		return ListenableFuture.class.isAssignableFrom(returnType.getParameterType());
	}

	@Override
	@SuppressWarnings("unchecked")
	public ListenableFuture<?> toListenableFuture(Object returnValue, MethodParameter returnType) {
		return (ListenableFuture<?>) returnValue;
	}

}
