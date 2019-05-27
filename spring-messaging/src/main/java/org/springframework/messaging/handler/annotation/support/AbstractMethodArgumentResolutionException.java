package org.springframework.messaging.handler.annotation.support;

import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.invocation.MethodArgumentResolutionException;

/**
 * 调用{@link org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver}导致的异常的基类.
 *
 * @deprecated as of 4.3.6, in favor of the invocation-associated
 * {@link MethodArgumentResolutionException}
 */
@Deprecated
@SuppressWarnings("serial")
public abstract class AbstractMethodArgumentResolutionException extends MethodArgumentResolutionException {

	protected AbstractMethodArgumentResolutionException(Message<?> message, MethodParameter parameter) {
		super(message, parameter);
	}

	protected AbstractMethodArgumentResolutionException(Message<?> message, MethodParameter parameter, String description) {
		super(message, parameter, description);
	}


	protected static String getMethodParamMessage(MethodParameter param) {
		return "";
	}

}
