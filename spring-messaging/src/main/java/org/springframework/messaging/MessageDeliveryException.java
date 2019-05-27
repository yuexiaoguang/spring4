package org.springframework.messaging;

/**
 * 表示消息传递过程中发生错误的异常.
 */
@SuppressWarnings("serial")
public class MessageDeliveryException extends MessagingException {

	public MessageDeliveryException(String description) {
		super(description);
	}

	public MessageDeliveryException(Message<?> undeliveredMessage) {
		super(undeliveredMessage);
	}

	public MessageDeliveryException(Message<?> undeliveredMessage, String description) {
		super(undeliveredMessage, description);
	}

	public MessageDeliveryException(Message<?> message, Throwable cause) {
		super(message, cause);
	}

	public MessageDeliveryException(Message<?> undeliveredMessage, String description, Throwable cause) {
		super(undeliveredMessage, description, cause);
	}

}
