package org.springframework.web.socket.client;

import java.net.URI;

import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;

/**
 * 启动WebSocket请求的约定.
 * 考虑使用声明式{@link WebSocketConnectionManager}作为替代方案, 它在应用程序启动时启动WebSocket连接到预先配置的URI.
 */
public interface WebSocketClient {

	ListenableFuture<WebSocketSession> doHandshake(WebSocketHandler webSocketHandler,
			String uriTemplate, Object... uriVariables);

	ListenableFuture<WebSocketSession> doHandshake(WebSocketHandler webSocketHandler,
			WebSocketHttpHeaders headers, URI uri);

}
