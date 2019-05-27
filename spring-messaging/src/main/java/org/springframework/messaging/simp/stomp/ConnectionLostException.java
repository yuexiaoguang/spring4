package org.springframework.messaging.simp.stomp;

/**
 * 当STOMP会话的连接丢失而不是关闭时引发.
 */
@SuppressWarnings("serial")
public class ConnectionLostException extends RuntimeException {

	public ConnectionLostException(String message) {
		super(message);
	}

}
