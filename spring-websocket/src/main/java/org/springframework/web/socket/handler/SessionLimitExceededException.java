package org.springframework.web.socket.handler;

import org.springframework.web.socket.CloseStatus;

/**
 * Raised when a WebSocket session has exceeded limits it has been configured
 * for, e.g. timeout, buffer size, etc.
 */
@SuppressWarnings("serial")
public class SessionLimitExceededException extends RuntimeException {

	private final CloseStatus status;


	public SessionLimitExceededException(String message, CloseStatus status) {
		super(message);
		this.status = (status != null) ? status : CloseStatus.NO_STATUS_CODE;
	}


	public CloseStatus getStatus() {
		return this.status;
	}

}
