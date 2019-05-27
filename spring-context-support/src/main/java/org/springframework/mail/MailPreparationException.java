package org.springframework.mail;

/**
 * 如果无法正确准备邮件, 则用户代码抛出异常, 例如无法为邮件文本呈现Velocity模板时.
 */
@SuppressWarnings("serial")
public class MailPreparationException extends MailException {

	public MailPreparationException(String msg) {
		super(msg);
	}

	public MailPreparationException(String msg, Throwable cause) {
		super(msg, cause);
	}

	public MailPreparationException(Throwable cause) {
		super("Could not prepare mail", cause);
	}
}
