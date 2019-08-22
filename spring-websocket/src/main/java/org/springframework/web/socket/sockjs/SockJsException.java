package org.springframework.web.socket.sockjs;

import org.springframework.core.NestedRuntimeException;

/**
 * 处理SockJS HTTP请求时引发的异常的基类.
 */
@SuppressWarnings("serial")
public class SockJsException extends NestedRuntimeException {

	private final String sessionId;


	public SockJsException(String message, Throwable cause) {
		this(message, null, cause);
	}

	public SockJsException(String message, String sessionId, Throwable cause) {
		super(message, cause);
		this.sessionId = sessionId;
	}


	/**
	 * 返回SockJS会话ID.
	 */
	public String getSockJsSessionId() {
		return this.sessionId;
	}

}
