package org.springframework.web.bind.annotation.support;

import java.lang.reflect.Method;

import org.springframework.core.NestedRuntimeException;

/**
 * Exception indicating that the execution of an annotated MVC handler method failed.
 *
 * @deprecated as of 4.3, in favor of the {@code HandlerMethod}-based MVC infrastructure
 */
@Deprecated
@SuppressWarnings("serial")
public class HandlerMethodInvocationException extends NestedRuntimeException {

	/**
	 * Create a new HandlerMethodInvocationException for the given Method handle and cause.
	 * @param handlerMethod the handler method handle
	 * @param cause the cause of the invocation failure
	 */
	public HandlerMethodInvocationException(Method handlerMethod, Throwable cause) {
		super("Failed to invoke handler method [" + handlerMethod + "]", cause);
	}

}
