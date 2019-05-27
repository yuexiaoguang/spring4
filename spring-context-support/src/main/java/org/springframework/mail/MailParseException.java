package org.springframework.mail;

/**
 * 如果遇到非法消息属性, 则抛出异常.
 */
@SuppressWarnings("serial")
public class MailParseException extends MailException {

	public MailParseException(String msg) {
		super(msg);
	}

	public MailParseException(String msg, Throwable cause) {
		super(msg, cause);
	}

	public MailParseException(Throwable cause) {
		super("Could not parse mail", cause);
	}
}
