package org.springframework.web.method.support;

import org.springframework.core.MethodParameter;

/**
 * A return value handler that supports async types. Such return value types
 * need to be handled with priority so the async value can be "unwrapped".
 *
 * <p><strong>Note: </strong> implementing this contract is not required but it
 * should be implemented when the handler needs to be prioritized ahead of others.
 * For example custom (async) handlers, by default ordered after built-in
 * handlers, should take precedence over {@code @ResponseBody} or
 * {@code @ModelAttribute} handling, which should occur once the async value is
 * ready.
 */
public interface AsyncHandlerMethodReturnValueHandler extends HandlerMethodReturnValueHandler {

	/**
	 * Whether the given return value represents asynchronous computation.
	 * @param returnValue the return value
	 * @param returnType the return type
	 * @return {@code true} if the return value is asynchronous.
	 */
	boolean isAsyncReturnValue(Object returnValue, MethodParameter returnType);

}
