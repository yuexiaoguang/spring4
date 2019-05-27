package org.springframework.jmx.access;

import org.springframework.jmx.JmxException;

/**
 * 由于MBeanServerConnection上的I/O问题而导致调用失败时抛出.
 */
@SuppressWarnings("serial")
public class MBeanConnectFailureException extends JmxException {

	public MBeanConnectFailureException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
