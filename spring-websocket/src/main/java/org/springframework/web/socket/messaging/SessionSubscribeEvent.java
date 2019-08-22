package org.springframework.web.socket.messaging;

import java.security.Principal;

import org.springframework.messaging.Message;

/**
 * 当使用简单消息传递协议(e.g. STOMP) 的新WebSocket客户端发送订阅请求时引发的事件.
 */
@SuppressWarnings("serial")
public class SessionSubscribeEvent extends AbstractSubProtocolEvent {

	public SessionSubscribeEvent(Object source, Message<byte[]> message) {
		super(source, message);
	}

	public SessionSubscribeEvent(Object source, Message<byte[]> message, Principal user) {
		super(source, message, user);
	}

}
