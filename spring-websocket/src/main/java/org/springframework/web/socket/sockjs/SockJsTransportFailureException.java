package org.springframework.web.socket.sockjs;

/**
 * 表示SockJS实现而不是用户代码中发生严重故障 (e.g. 写入响应时出现IOException).
 * 引发此异常时, 通常会关闭SockJS会话.
 */
@SuppressWarnings("serial")
public class SockJsTransportFailureException extends SockJsException {

	public SockJsTransportFailureException(String message, Throwable cause) {
		super(message, cause);
	}

	public SockJsTransportFailureException(String message, String sessionId, Throwable cause) {
		super(message, sessionId, cause);
	}
}
