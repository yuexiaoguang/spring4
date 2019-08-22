package org.springframework.web.socket.handler;

import java.io.IOException;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;

/**
 * {@link WebSocketHandler}实现的便捷基类, 仅用于处理二进制消息.
 *
 * <p>使用{@link CloseStatus#NOT_ACCEPTABLE}拒绝文本消息.
 * 所有其他方法都有空实现.
 */
public class BinaryWebSocketHandler extends AbstractWebSocketHandler {

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) {
		try {
			session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Text messages not supported"));
		}
		catch (IOException ex) {
			// ignore
		}
	}

}
