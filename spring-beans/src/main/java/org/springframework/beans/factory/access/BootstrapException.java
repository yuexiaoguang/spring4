package org.springframework.beans.factory.access;

import org.springframework.beans.FatalBeanException;

/**
 * 如果引导类无法加载bean工厂, 则抛出异常.
 */
@SuppressWarnings("serial")
public class BootstrapException extends FatalBeanException {

	public BootstrapException(String msg) {
		super(msg);
	}

	public BootstrapException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
