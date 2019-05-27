package org.springframework.messaging.converter;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;

/**
 * {@link MessageConverter}实现引发的异常.
 */
@SuppressWarnings("serial")
public class MessageConversionException extends MessagingException {

	public MessageConversionException(String description) {
		super(description);
	}

	public MessageConversionException(String description, Throwable cause) {
		super(description, cause);
	}

	public MessageConversionException(Message<?> failedMessage, String description) {
		super(failedMessage, description);
	}

	public MessageConversionException(Message<?> failedMessage, String description, Throwable cause) {
		super(failedMessage, description, cause);
	}

}
