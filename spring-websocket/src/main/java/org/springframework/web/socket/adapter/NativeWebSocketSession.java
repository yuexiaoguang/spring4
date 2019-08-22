package org.springframework.web.socket.adapter;

import org.springframework.web.socket.WebSocketSession;

/**
 * 一个{@link WebSocketSession}, 通过getter公开底层的本机WebSocketSession.
 */
public interface NativeWebSocketSession extends WebSocketSession {

	/**
	 * 返回底层本机WebSocketSession.
	 */
	Object getNativeSession();

	/**
	 * 返回底层本机WebSocketSession.
	 * 
	 * @param requiredType 所需的会话类型
	 * 
	 * @return 所需类型的本机会话, 或{@code null}
	 */
	<T> T getNativeSession(Class<T> requiredType);

}
