package org.springframework.messaging.simp.annotation.support;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;

@SuppressWarnings("serial")
public class MissingSessionUserException extends MessagingException {

	public MissingSessionUserException(Message<?> message) {
		super(message, "No \"user\" header in message");
	}

}
