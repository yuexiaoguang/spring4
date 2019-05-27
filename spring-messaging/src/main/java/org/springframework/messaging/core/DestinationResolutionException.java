package org.springframework.messaging.core;

import org.springframework.messaging.MessagingException;

/**
 * 当{@link DestinationResolver}无法解析目标时抛出.
 */
@SuppressWarnings("serial")
public class DestinationResolutionException extends MessagingException {

	public DestinationResolutionException(String description) {
		super(description);
	}

	public DestinationResolutionException(String description, Throwable cause) {
		super(description, cause);
	}

}
