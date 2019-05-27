package org.springframework.messaging;

import org.springframework.core.NestedRuntimeException;

/**
 * 与消息相关的任何失败的基本异常.
 */
@SuppressWarnings("serial")
public class MessagingException extends NestedRuntimeException {

	private final Message<?> failedMessage;


	public MessagingException(Message<?> message) {
		super(null);
		this.failedMessage = message;
	}

	public MessagingException(String description) {
		super(description);
		this.failedMessage = null;
	}

	public MessagingException(String description, Throwable cause) {
		super(description, cause);
		this.failedMessage = null;
	}

	public MessagingException(Message<?> message, String description) {
		super(description);
		this.failedMessage = message;
	}

	public MessagingException(Message<?> message, Throwable cause) {
		super(null, cause);
		this.failedMessage = message;
	}

	public MessagingException(Message<?> message, String description, Throwable cause) {
		super(description, cause);
		this.failedMessage = message;
	}


	public Message<?> getFailedMessage() {
		return this.failedMessage;
	}

	@Override
	public String toString() {
		return super.toString() + (this.failedMessage == null ? ""
				: (", failedMessage=" + this.failedMessage));
	}

}
