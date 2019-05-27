package org.springframework.messaging;

/**
 * 表示消息处理期间发生错误的异常.
 */
@SuppressWarnings("serial")
public class MessageHandlingException extends MessagingException {

	public MessageHandlingException(Message<?> failedMessage) {
		super(failedMessage);
	}

	public MessageHandlingException(Message<?> message, String description) {
		super(message, description);
	}

	public MessageHandlingException(Message<?> failedMessage, Throwable cause) {
		super(failedMessage, cause);
	}

	public MessageHandlingException(Message<?> message, String description, Throwable cause) {
		super(message, description, cause);
	}

}
