package org.springframework.web.socket.config.annotation;

import org.springframework.web.socket.WebSocketHandler;

/**
 * 定义回调方法以通过
 * {@link org.springframework.web.socket.config.annotation.EnableWebSocket @EnableWebSocket}
 * 配置WebSocket请求处理.
 */
public interface WebSocketConfigurer {

	/**
	 * 如果需要，注册包括SockJS后备选项的{@link WebSocketHandler}.
	 */
	void registerWebSocketHandlers(WebSocketHandlerRegistry registry);

}
