package org.springframework.web.bind.annotation.support;

import java.lang.reflect.Method;

import org.springframework.core.NestedRuntimeException;

/**
 * 表示执行带注解的MVC处理器方法失败.
 *
 * @deprecated 从4.3开始, 支持基于{@code HandlerMethod}的MVC基础结构
 */
@Deprecated
@SuppressWarnings("serial")
public class HandlerMethodInvocationException extends NestedRuntimeException {

	/**
	 * @param handlerMethod the handler method handle
	 * @param cause the cause of the invocation failure
	 */
	public HandlerMethodInvocationException(Method handlerMethod, Throwable cause) {
		super("Failed to invoke handler method [" + handlerMethod + "]", cause);
	}

}
