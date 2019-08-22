package org.springframework.web.socket.config.annotation;

import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;
import org.springframework.web.util.UrlPathHelper;

/**
 * 通过WebSocket端点注册STOMP的约定.
 */
public interface StompEndpointRegistry {

	/**
	 * 在给定的映射路径上通过WebSocket端点注册STOMP.
	 */
	StompWebSocketEndpointRegistration addEndpoint(String... paths);

	/**
	 * 设置用于STOMP端点的{@link org.springframework.web.servlet.HandlerMapping}的顺序,
	 * 相对于其他Spring MVC处理器映射.
	 * <p>默认为 1.
	 */
	void setOrder(int order);

	/**
	 * 配置STOMP端点的{@link org.springframework.web.servlet.HandlerMapping HandlerMapping}的自定义的{@link UrlPathHelper}.
	 */
	void setUrlPathHelper(UrlPathHelper urlPathHelper);

	/**
	 * 配置处理器以自定义或处理客户端的STOMP ERROR帧.
	 * 
	 * @param errorHandler 错误处理器
	 */
	WebMvcStompEndpointRegistry setErrorHandler(StompSubProtocolErrorHandler errorHandler);

}