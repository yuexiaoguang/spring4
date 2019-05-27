package org.springframework.web.socket.client;

import java.net.URI;

import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;

/**
 * Contract for initiating a WebSocket request. As an alternative considering using the
 * declarative style {@link WebSocketConnectionManager} that starts a WebSocket connection
 * to a pre-configured URI when the application starts.
 */
public interface WebSocketClient {

	ListenableFuture<WebSocketSession> doHandshake(WebSocketHandler webSocketHandler,
			String uriTemplate, Object... uriVariables);

	ListenableFuture<WebSocketSession> doHandshake(WebSocketHandler webSocketHandler,
			WebSocketHttpHeaders headers, URI uri);

}
