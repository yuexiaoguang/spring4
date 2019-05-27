package org.springframework.jmx.access;

import org.springframework.jmx.JmxException;

/**
 * 当MBean资源上的调用因异常而失败时抛出 (反射异常或目标方法本身抛出的异常).
 */
@SuppressWarnings("serial")
public class InvocationFailureException extends JmxException {

	public InvocationFailureException(String msg) {
		super(msg);
	}

	public InvocationFailureException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
