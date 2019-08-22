package org.springframework.web.socket.server;

import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.handler.PerConnectionWebSocketHandler;

/**
 * 处理WebSocket握手请求的约定.
 */
public interface HandshakeHandler {

	/**
	 * 发起握手.
	 * 
	 * @param request 当前的请求
	 * @param response 当前的响应
	 * @param wsHandler 处理WebSocket消息的处理器; 请参阅{@link PerConnectionWebSocketHandler}以提供具有每个连接生命周期的处理器.
	 * @param attributes 来自HTTP握手的属性, 要与WebSocket会话关联; 复制提供的属性, 不使用原始Map.
	 * 
	 * @return whether 握手协商成功与否.
	 * 在任何一种情况下, 响应状态, header和正文都将更新以反映协商的结果
	 * @throws HandshakeFailureException 由于内部不可恢复的错误, 握手处理未能完成时抛出,
	 * i.e. 服务器错误, 而不是握手协商失败.
	 */
	boolean doHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
			Map<String, Object> attributes) throws HandshakeFailureException;

}
