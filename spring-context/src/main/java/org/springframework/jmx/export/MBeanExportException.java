package org.springframework.jmx.export;

import org.springframework.jmx.JmxException;

/**
 * 导出MBean错误异常.
 */
@SuppressWarnings("serial")
public class MBeanExportException extends JmxException {

	public MBeanExportException(String msg) {
		super(msg);
	}

	public MBeanExportException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
