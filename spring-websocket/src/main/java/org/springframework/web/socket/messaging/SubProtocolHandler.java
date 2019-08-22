package org.springframework.web.socket.messaging;

import java.util.List;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * 处理WebSocket消息的约定, 作为更高级别协议的一部分, 在WebSocket RFC规范中称为"子协议".
 * 处理来自客户端的{@link WebSocketMessage}以及发送到客户端的{@link Message}.
 *
 * <p>可以在{@link SubProtocolWebSocketHandler}上配置此接口的实现,
 * 该接口选择子协议处理器, 以根据客户端通过{@code Sec-WebSocket-Protocol}请求 header请求的子协议来委派消息.
 */
public interface SubProtocolHandler {

	/**
	 * 返回此处理器支持的子协议列表 (never {@code null}).
	 */
	List<String> getSupportedProtocols();

	/**
	 * 处理从客户端收到的给定{@link WebSocketMessage}.
	 * 
	 * @param session 客户端会话
	 * @param message 客户端消息
	 * @param outputChannel 用于发送消息的输出Channel
	 */
	void handleMessageFromClient(WebSocketSession session, WebSocketMessage<?> message, MessageChannel outputChannel)
			throws Exception;

	/**
	 * 将给定的{@link Message}处理到与给定WebSocket会话关联的客户端.
	 * 
	 * @param session 客户端会话
	 * @param message 客户端消息
	 */
	void handleMessageToClient(WebSocketSession session, Message<?> message) throws Exception;

	/**
	 * 从给定消息中解析会话ID或返回{@code null}.
	 * 
	 * @param message 从中解析会话ID的消息
	 */
	String resolveSessionId(Message<?> message);

	/**
	 * 在{@link WebSocketSession}启动后调用.
	 * 
	 * @param session 客户端会话
	 * @param outputChannel a channel
	 */
	void afterSessionStarted(WebSocketSession session, MessageChannel outputChannel) throws Exception;

	/**
	 * 在{@link WebSocketSession}结束后调用.
	 * 
	 * @param session 客户端会话
	 * @param closeStatus 会话关闭的原因
	 * @param outputChannel a channel
	 */
	void afterSessionEnded(WebSocketSession session, CloseStatus closeStatus, MessageChannel outputChannel)
			throws Exception;

}
