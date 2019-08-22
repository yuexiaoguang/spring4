package org.springframework.web.socket.handler;

import org.springframework.web.socket.CloseStatus;

/**
 * 当WebSocket会话超出已配置的限制时引发, e.g. 超时, 缓冲区大小等.
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
