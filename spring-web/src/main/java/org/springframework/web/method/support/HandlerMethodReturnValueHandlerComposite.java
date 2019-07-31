package org.springframework.web.method.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * 通过委托给已注册的{@link HandlerMethodReturnValueHandler}列表来处理方法返回值.
 * 缓存先前已解析的返回类型以便更快地进行查找.
 */
public class HandlerMethodReturnValueHandlerComposite implements AsyncHandlerMethodReturnValueHandler {

	protected final Log logger = LogFactory.getLog(getClass());

	private final List<HandlerMethodReturnValueHandler> returnValueHandlers =
		new ArrayList<HandlerMethodReturnValueHandler>();


	/**
	 * 返回包含注册的处理器的只读列表, 或空列表.
	 */
	public List<HandlerMethodReturnValueHandler> getHandlers() {
		return Collections.unmodifiableList(this.returnValueHandlers);
	}

	/**
	 * 是否所有已注册的{@link HandlerMethodReturnValueHandler}都支持给定的{@linkplain MethodParameter 方法返回类型}.
	 */
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

	/**
	 * 迭代已注册的{@link HandlerMethodReturnValueHandler}, 并调用支持它的那个.
	 * 
	 * @throws IllegalStateException 如果找不到合适的{@link HandlerMethodReturnValueHandler}.
	 */
	@Override
	public void handleReturnValue(Object returnValue, MethodParameter returnType,
			ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {

		HandlerMethodReturnValueHandler handler = selectHandler(returnValue, returnType);
		if (handler == null) {
			throw new IllegalArgumentException("Unknown return value type: " + returnType.getParameterType().getName());
		}
		handler.handleReturnValue(returnValue, returnType, mavContainer, webRequest);
	}

	private HandlerMethodReturnValueHandler selectHandler(Object value, MethodParameter returnType) {
		boolean isAsyncValue = isAsyncReturnValue(value, returnType);
		for (HandlerMethodReturnValueHandler handler : this.returnValueHandlers) {
			if (isAsyncValue && !(handler instanceof AsyncHandlerMethodReturnValueHandler)) {
				continue;
			}
			if (handler.supportsReturnType(returnType)) {
				return handler;
			}
		}
		return null;
	}

	@Override
	public boolean isAsyncReturnValue(Object value, MethodParameter returnType) {
		for (HandlerMethodReturnValueHandler handler : this.returnValueHandlers) {
			if (handler instanceof AsyncHandlerMethodReturnValueHandler) {
				if (((AsyncHandlerMethodReturnValueHandler) handler).isAsyncReturnValue(value, returnType)) {
					return true;
				}
			}
		}
		return false;
	}

	public HandlerMethodReturnValueHandlerComposite addHandler(HandlerMethodReturnValueHandler handler) {
		this.returnValueHandlers.add(handler);
		return this;
	}

	public HandlerMethodReturnValueHandlerComposite addHandlers(List<? extends HandlerMethodReturnValueHandler> handlers) {
		if (handlers != null) {
			for (HandlerMethodReturnValueHandler handler : handlers) {
				this.returnValueHandlers.add(handler);
			}
		}
		return this;
	}

}
