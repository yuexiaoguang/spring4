package org.springframework.web.socket.config.annotation;

import org.springframework.web.socket.WebSocketHandler;

/**
 * 提供配置{@link WebSocketHandler}请求映射的方法.
 */
public interface WebSocketHandlerRegistry {

	/**
	 * 在指定的URL路径上配置WebSocketHandler.
	 */
	WebSocketHandlerRegistration addHandler(WebSocketHandler webSocketHandler, String... paths);

}