package org.springframework.web.socket.server;

import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;

/**
 * 用于WebSocket握手请求的拦截器.
 * 可用于检查握手请求和响应, 以及将属性传递给目标{@link WebSocketHandler}.
 */
public interface HandshakeInterceptor {

	/**
	 * 在处理握手之前调用.
	 * 
	 * @param request 当前的请求
	 * @param response 当前的响应
	 * @param wsHandler 目标WebSocket处理器
	 * @param attributes 来自HTTP握手的属性, 要与WebSocket会话关联; 复制提供的属性, 不使用原始Map.
	 * 
	 * @return 是否继续握手 ({@code true}) 或中止 ({@code false})
	 */
	boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception;

	/**
	 * 握手完成后调用.
	 * 响应状态和header指示握手的结果, i.e. 它是否成功.
	 * 
	 * @param request 当前的请求
	 * @param response 当前的响应
	 * @param wsHandler 目标WebSocket处理器
	 * @param exception 握手期间引发的异常, 或{@code null}
	 */
	void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler wsHandler, Exception exception);

}
