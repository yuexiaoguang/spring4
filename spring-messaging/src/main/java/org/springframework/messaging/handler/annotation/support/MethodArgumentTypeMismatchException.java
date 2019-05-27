package org.springframework.messaging.handler.annotation.support;

import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;

/**
 * 表示方法参数不具有预期类型的​​异常.
 */
@SuppressWarnings({"serial", "deprecation"})
public class MethodArgumentTypeMismatchException extends AbstractMethodArgumentResolutionException {

	public MethodArgumentTypeMismatchException(Message<?> message, MethodParameter parameter, String description) {
		super(message, parameter, description);
	}

}
