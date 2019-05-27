package org.springframework.web.socket.config.annotation;

import org.springframework.web.socket.WebSocketHandler;

/**
 * Provides methods for configuring {@link WebSocketHandler} request mappings.
 */
public interface WebSocketHandlerRegistry {

	/**
	 * Configure a WebSocketHandler at the specified URL paths.
	 */
	WebSocketHandlerRegistration addHandler(WebSocketHandler webSocketHandler, String... paths);

}