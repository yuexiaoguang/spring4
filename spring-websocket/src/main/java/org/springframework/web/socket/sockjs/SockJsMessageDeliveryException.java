package org.springframework.web.socket.sockjs;

import java.util.List;

/**
 * 通过HTTP POST成功接收并解析的消息帧, 由于处理器失败或连接已关闭,
 * 而无法将其包含的一条或多条消息传递给WebSocketHandler时引发的异常.
 *
 * <p>此异常后SockJS会话不会自动关闭.
 */
@SuppressWarnings("serial")
public class SockJsMessageDeliveryException extends SockJsException {

	private final List<String> undeliveredMessages;


	public SockJsMessageDeliveryException(String sessionId, List<String> undeliveredMessages, Throwable cause) {
		super("Failed to deliver message(s) " + undeliveredMessages + " for session " + sessionId, sessionId, cause);
		this.undeliveredMessages = undeliveredMessages;
	}

	public SockJsMessageDeliveryException(String sessionId, List<String> undeliveredMessages, String message) {
		super("Failed to deliver message(s) " + undeliveredMessages + " for session "
				+ sessionId + ": " + message, sessionId, null);
		this.undeliveredMessages = undeliveredMessages;
	}

	public List<String> getUndeliveredMessages() {
		return this.undeliveredMessages;
	}

}
