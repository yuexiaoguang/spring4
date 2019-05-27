package org.springframework.web.socket.server;

import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.handler.PerConnectionWebSocketHandler;

/**
 * Contract for processing a WebSocket handshake request.
 */
public interface HandshakeHandler {

	/**
	 * Initiate the handshake.
	 * @param request the current request
	 * @param response the current response
	 * @param wsHandler the handler to process WebSocket messages; see
	 * {@link PerConnectionWebSocketHandler} for providing a handler with
	 * per-connection lifecycle.
	 * @param attributes attributes from the HTTP handshake to associate with the WebSocket
	 * session; the provided attributes are copied, the original map is not used.
	 * @return whether the handshake negotiation was successful or not. In either case the
	 * response status, headers, and body will have been updated to reflect the
	 * result of the negotiation
	 * @throws HandshakeFailureException thrown when handshake processing failed to
	 * complete due to an internal, unrecoverable error, i.e. a server error as
	 * opposed to a failure to successfully negotiate the handshake.
	 */
	boolean doHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
			Map<String, Object> attributes) throws HandshakeFailureException;

}
