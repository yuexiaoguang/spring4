package org.springframework.messaging.handler.invocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * 包装并委托给其他的HandlerMethodReturnValueHandler.
 */
public class HandlerMethodReturnValueHandlerComposite implements AsyncHandlerMethodReturnValueHandler {

	private static final Log logger = LogFactory.getLog(HandlerMethodReturnValueHandlerComposite.class);

	private final List<HandlerMethodReturnValueHandler> returnValueHandlers = new ArrayList<HandlerMethodReturnValueHandler>();


	/**
	 * 返回配置的处理器的只读列表.
	 */
	public List<HandlerMethodReturnValueHandler> getReturnValueHandlers() {
		return Collections.unmodifiableList(this.returnValueHandlers);
	}

	/**
	 * 清空配置的处理器列表.
	 */
	public void clear() {
		this.returnValueHandlers.clear();
	}

	/**
	 * 添加给定的{@link HandlerMethodReturnValueHandler}.
	 */
	public HandlerMethodReturnValueHandlerComposite addHandler(HandlerMethodReturnValueHandler returnValuehandler) {
		this.returnValueHandlers.add(returnValuehandler);
		return this;
	}

	/**
	 * 添加给定的{@link HandlerMethodReturnValueHandler}.
	 */
	public HandlerMethodReturnValueHandlerComposite addHandlers(List<? extends HandlerMethodReturnValueHandler> handlers) {
		if (handlers != null) {
			for (HandlerMethodReturnValueHandler handler : handlers) {
				this.returnValueHandlers.add(handler);
			}
		}
		return this;
	}

	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		return getReturnValueHandler(returnType) != null;
	}

	private HandlerMethodReturnValueHandler getReturnValueHandler(MethodParameter returnType) {
		for (HandlerMethodReturnValueHandler handler : this.returnValueHandlers) {
			if (handler.supportsReturnType(returnType)) {
				return handler;
			}
		}
		return null;
	}

	@Override
	public void handleReturnValue(Object returnValue, MethodParameter returnType, Message<?> message)
			throws Exception {

		HandlerMethodReturnValueHandler handler = getReturnValueHandler(returnType);
		if (handler == null) {
			throw new IllegalStateException("No handler for return value type: " + returnType.getParameterType());
		}
		if (logger.isTraceEnabled()) {
			logger.trace("Processing return value with " + handler);
		}
		handler.handleReturnValue(returnValue, returnType, message);
	}

	@Override
	public boolean isAsyncReturnValue(Object returnValue, MethodParameter returnType) {
		HandlerMethodReturnValueHandler handler = getReturnValueHandler(returnType);
		return (handler instanceof AsyncHandlerMethodReturnValueHandler &&
				((AsyncHandlerMethodReturnValueHandler) handler).isAsyncReturnValue(returnValue, returnType));
	}

	@Override
	public ListenableFuture<?> toListenableFuture(Object returnValue, MethodParameter returnType) {
		HandlerMethodReturnValueHandler handler = getReturnValueHandler(returnType);
		Assert.state(handler instanceof AsyncHandlerMethodReturnValueHandler,
				"AsyncHandlerMethodReturnValueHandler required");
		return ((AsyncHandlerMethodReturnValueHandler) handler).toListenableFuture(returnValue, returnType);
	}

}
