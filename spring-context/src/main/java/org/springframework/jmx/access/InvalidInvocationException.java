package org.springframework.jmx.access;

import javax.management.JMRuntimeException;

/**
 * 尝试通过代理的MBean资源的管理接口, 在未公开的代理上调用操作时抛出.
 */
@SuppressWarnings("serial")
public class InvalidInvocationException extends JMRuntimeException {

	public InvalidInvocationException(String msg) {
		super(msg);
	}

}
