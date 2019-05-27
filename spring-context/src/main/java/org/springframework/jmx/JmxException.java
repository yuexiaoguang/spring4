package org.springframework.jmx;

import org.springframework.core.NestedRuntimeException;

/**
 * JMX错误引发的常规基本异常.
 * 非受检的, 因为JMX失败通常是致命的.
 */
@SuppressWarnings("serial")
public class JmxException extends NestedRuntimeException {

	public JmxException(String msg) {
		super(msg);
	}

	public JmxException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
