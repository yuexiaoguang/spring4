package org.springframework.jmx.access;

import org.springframework.jmx.JmxException;

/**
 * 如果在尝试检索MBean元数据时遇到异常, 则抛出该异常.
 */
@SuppressWarnings("serial")
public class MBeanInfoRetrievalException extends JmxException {

	public MBeanInfoRetrievalException(String msg) {
		super(msg);
	}

	public MBeanInfoRetrievalException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
