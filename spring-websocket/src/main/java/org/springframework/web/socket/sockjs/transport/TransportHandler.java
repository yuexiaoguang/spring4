package org.springframework.web.socket.sockjs.transport;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.sockjs.SockJsException;
import org.springframework.web.socket.sockjs.SockJsService;

/**
 * 处理SockJS会话URL, i.e. 特定于传输的请求.
 */
public interface TransportHandler {

	/**
	 * 使用给定的配置初始化此处理器.
	 * 
	 * @param serviceConfig 包含
	 * {@link org.springframework.web.socket.sockjs.SockJsService}定义的配置
	 */
	void initialize(SockJsServiceConfig serviceConfig);

	/**
	 * 返回此处理器支持的传输类型.
	 */
	TransportType getTransportType();

	/**
	 * 检查给定会话的类型是否与此{@code TransportHandler}的传输类型匹配, 其中会话ID和传输类型是从SockJS URL中提取的.
	 * 
	 * @return {@code true} 如果会话匹配 (因此会被{@link #handleRequest}接受), 否则{@code false}
	 */
	boolean checkSessionType(SockJsSession session);

	/**
	 * 处理给定的请求并将消息委托给提供的{@link WebSocketHandler}.
	 * 
	 * @param request 当前的请求
	 * @param response 当前的响应
	 * @param handler 目标WebSocketHandler (never {@code null})
	 * @param session SockJS会话 (never {@code null})
	 * 
	 * @throws SockJsException 请求处理失败时引发, 如{@link SockJsService}中所述
	 */
	void handleRequest(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler handler, SockJsSession session) throws SockJsException;

}
