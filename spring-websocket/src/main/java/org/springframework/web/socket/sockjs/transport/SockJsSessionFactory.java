package org.springframework.web.socket.sockjs.transport;

import java.util.Map;

import org.springframework.web.socket.WebSocketHandler;

/**
 * 用于创建SockJS会话的工厂.
 * {@link TransportHandler}通常也用作SockJS会话工厂.
 */
public interface SockJsSessionFactory {

	/**
	 * 创建一个新的SockJS会话.
	 * 
	 * @param sessionId 会话的ID
	 * @param handler 底层{@link WebSocketHandler}
	 * @param attributes 握手请求特定属性
	 * 
	 * @return 新的会话, never {@code null}
	 */
	SockJsSession createSession(String sessionId, WebSocketHandler handler, Map<String, Object> attributes);

}
