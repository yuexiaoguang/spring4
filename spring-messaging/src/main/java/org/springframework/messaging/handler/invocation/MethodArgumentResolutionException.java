package org.springframework.messaging.handler.invocation;

import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;

/**
 * 调用{@link org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver}导致的常见异常.
 */
@SuppressWarnings("serial")
public class MethodArgumentResolutionException extends MessagingException {

	private final MethodParameter parameter;


	/**
	 * 提供无效{@code MethodParameter}.
	 */
	public MethodArgumentResolutionException(Message<?> message, MethodParameter parameter) {
		super(message, getMethodParameterMessage(parameter));
		this.parameter = parameter;
	}

	/**
	 * 提供无效的{@code MethodParameter}和准备好的描述.
	 */
	public MethodArgumentResolutionException(Message<?> message, MethodParameter parameter, String description) {
		super(message, getMethodParameterMessage(parameter) + ": " + description);
		this.parameter = parameter;
	}


	/**
	 * 返回被拒绝的MethodParameter.
	 */
	public final MethodParameter getMethodParameter() {
		return this.parameter;
	}


	private static String getMethodParameterMessage(MethodParameter parameter) {
		return "Could not resolve method parameter at index " + parameter.getParameterIndex() +
				" in " + parameter.getMethod().toGenericString();
	}

}
