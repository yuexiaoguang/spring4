package org.springframework.web.socket.server;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketHandler;

/**
 * 用于执行实际升级到WebSocket交换的特定于服务器的策略.
 */
public interface RequestUpgradeStrategy {

	/**
	 * 返回支持的WebSocket协议版本.
	 */
	String[] getSupportedVersions();

	/**
	 * 返回底层WebSocket服务器支持的WebSocket协议扩展.
	 */
	List<WebSocketExtension> getSupportedExtensions(ServerHttpRequest request);

	/**
	 * 执行特定于运行时的步骤以完成升级.
	 * 成功协商握手请求后调用.
	 * 
	 * @param request 当前的请求
	 * @param response 当前的响应
	 * @param selectedProtocol 选定的子协议
	 * @param selectedExtensions 选定的WebSocket协议扩展
	 * @param user 要与WebSocket会话关联的用户
	 * @param wsHandler WebSocket消息的处理器
	 * @param attributes 握手请求通过{@link org.springframework.web.socket.server.HandshakeInterceptor}
	 * 在WebSocket会话上设置的特定属性, 从而可用于{@link org.springframework.web.socket.WebSocketHandler}
	 * 
	 * @throws HandshakeFailureException 由于内部不可恢复的错误, 握手处理未能完成时抛出,
	 * i.e. 服务器错误, 而不是协商握手请求的要求的失败.
	 */
	void upgrade(ServerHttpRequest request, ServerHttpResponse response,
			String selectedProtocol, List<WebSocketExtension> selectedExtensions, Principal user,
			WebSocketHandler wsHandler, Map<String, Object> attributes) throws HandshakeFailureException;

}
