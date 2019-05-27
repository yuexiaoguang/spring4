package org.springframework.jmx.export;

/**
 * 当无法注册MBean时抛出异常, 例如由于命名冲突.
 */
@SuppressWarnings("serial")
public class UnableToRegisterMBeanException extends MBeanExportException {

	public UnableToRegisterMBeanException(String msg) {
		super(msg);
	}

	public UnableToRegisterMBeanException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
