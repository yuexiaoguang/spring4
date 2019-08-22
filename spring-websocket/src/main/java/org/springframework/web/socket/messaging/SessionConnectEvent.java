package org.springframework.web.socket.messaging;

import java.security.Principal;

import org.springframework.messaging.Message;

/**
 * 使用简单消息传递协议 (e.g. STOMP) 作为WebSocket子协议的新WebSocket客户端发出连接请求时引发的事件.
 *
 * <p>请注意, 这与建立的WebSocket会话不同, 而是客户端首次尝试在子协议内进行连接, 例如发送STOMP CONNECT帧.
 */
@SuppressWarnings("serial")
public class SessionConnectEvent extends AbstractSubProtocolEvent {

	/**
	 * @param source 发布事件的组件 (never {@code null})
	 * @param message 连接消息
	 */
	public SessionConnectEvent(Object source, Message<byte[]> message) {
		super(source, message);
	}

	public SessionConnectEvent(Object source, Message<byte[]> message, Principal user) {
		super(source, message, user);
	}

}
