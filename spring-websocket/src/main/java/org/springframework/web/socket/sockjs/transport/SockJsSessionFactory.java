package org.springframework.web.socket.sockjs.transport;

import java.util.Map;

import org.springframework.web.socket.WebSocketHandler;

/**
 * A factory for creating a SockJS session. {@link TransportHandler}s typically also serve
 * as SockJS session factories.
 */
public interface SockJsSessionFactory {

	/**
	 * Create a new SockJS session.
	 * @param sessionId the ID of the session
	 * @param handler the underlying {@link WebSocketHandler}
	 * @param attributes handshake request specific attributes
	 * @return a new session, never {@code null}
	 */
	SockJsSession createSession(String sessionId, WebSocketHandler handler, Map<String, Object> attributes);

}
