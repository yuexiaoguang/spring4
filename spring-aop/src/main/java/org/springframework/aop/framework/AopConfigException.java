package org.springframework.aop.framework;

import org.springframework.core.NestedRuntimeException;

/**
 * 非法AOP配置参数的异常.
 */
@SuppressWarnings("serial")
public class AopConfigException extends NestedRuntimeException {

	public AopConfigException(String msg) {
		super(msg);
	}

	public AopConfigException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
