package org.springframework.web.socket.sockjs.transport;

import org.springframework.web.socket.WebSocketSession;

/**
 * SockJS extension of Spring's standard {@link WebSocketSession}.
 */
public interface SockJsSession extends WebSocketSession {

	/**
	 * Return the time (in ms) since the session was last active, or otherwise
	 * if the session is new, then the time since the session was created.
	 */
	long getTimeSinceLastActive();

	/**
	 * Disable the SockJS heartbeat, presumably because a higher-level protocol
	 * has heartbeats enabled for the session already. It is not recommended to
	 * disable this otherwise, as it helps proxies to know the connection is
	 * not hanging.
	 */
	void disableHeartbeat();

}
