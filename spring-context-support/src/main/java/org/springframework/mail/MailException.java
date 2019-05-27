package org.springframework.mail;

import org.springframework.core.NestedRuntimeException;

/**
 * 所有邮件异常的基类.
 */
@SuppressWarnings("serial")
public abstract class MailException extends NestedRuntimeException {

	public MailException(String msg) {
		super(msg);
	}

	public MailException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
