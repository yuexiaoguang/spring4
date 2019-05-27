package org.springframework.context;

import org.springframework.beans.FatalBeanException;

/**
 * 应用程序上下文初始化期间抛出异常.
 */
@SuppressWarnings("serial")
public class ApplicationContextException extends FatalBeanException {

	public ApplicationContextException(String msg) {
		super(msg);
	}

	public ApplicationContextException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
