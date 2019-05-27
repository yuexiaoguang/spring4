package org.springframework.aop;

import org.springframework.core.NestedRuntimeException;

/**
 * 由于配置错误或意外的运行时问题导致AOP调用失败时抛出的异常.
 */
@SuppressWarnings("serial")
public class AopInvocationException extends NestedRuntimeException {

	public AopInvocationException(String msg) {
		super(msg);
	}

	public AopInvocationException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
