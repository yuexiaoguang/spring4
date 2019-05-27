package org.springframework.messaging.handler.invocation;

import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;

/**
 * {@link AsyncHandlerMethodReturnValueHandler}实现的基类, 它只支持异步 (类似Future)的返回值,
 * 仅作为Spring的{@link org.springframework.util.concurrent.ListenableFuture ListenableFuture}的类型的适配器.
 */
public abstract class AbstractAsyncReturnValueHandler implements AsyncHandlerMethodReturnValueHandler {

	@Override
	public boolean isAsyncReturnValue(Object returnValue, MethodParameter returnType) {
		return true;
	}

	@Override
	public void handleReturnValue(Object returnValue, MethodParameter returnType, Message<?> message) {
		// 永远不应该调用, 因为从isAsyncReturnValue返回"true"
		throw new IllegalStateException("Unexpected invocation");
	}

}
