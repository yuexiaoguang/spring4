package org.springframework.jms.support.destination;

import org.springframework.jms.JmsException;

/**
 * 当DestinationResolver无法解析目标名称时抛出.
 */
@SuppressWarnings("serial")
public class DestinationResolutionException extends JmsException {

	public DestinationResolutionException(String msg) {
		super(msg);
	}

	public DestinationResolutionException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
