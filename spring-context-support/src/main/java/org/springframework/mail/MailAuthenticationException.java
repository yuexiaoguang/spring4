package org.springframework.mail;

/**
 * 验证失败时抛出异常.
 */
@SuppressWarnings("serial")
public class MailAuthenticationException extends MailException {

	public MailAuthenticationException(String msg) {
		super(msg);
	}

	public MailAuthenticationException(String msg, Throwable cause) {
		super(msg, cause);
	}

	public MailAuthenticationException(Throwable cause) {
		super("Authentication failed", cause);
	}
}
