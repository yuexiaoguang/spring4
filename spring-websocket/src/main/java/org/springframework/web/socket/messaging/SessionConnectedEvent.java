package org.springframework.web.socket.messaging;

import java.security.Principal;

import org.springframework.messaging.Message;

/**
 * 连接事件, 表示服务器对客户端连接请求的响应.
 * See {@link org.springframework.web.socket.messaging.SessionConnectEvent}.
 */
@SuppressWarnings("serial")
public class SessionConnectedEvent extends AbstractSubProtocolEvent {

	/**
	 * @param source 发布事件的组件 (never {@code null})
	 * @param message 连接的消息
	 */
	public SessionConnectedEvent(Object source, Message<byte[]> message) {
		super(source, message);
	}

	public SessionConnectedEvent(Object source, Message<byte[]> message, Principal user) {
		super(source, message, user);
	}

}
