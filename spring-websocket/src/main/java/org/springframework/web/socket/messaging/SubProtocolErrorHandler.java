package org.springframework.web.socket.messaging;

import org.springframework.messaging.Message;

/**
 * 处理发送给客户端的子协议错误的约定.
 */
public interface SubProtocolErrorHandler<P> {

	/**
	 * 处理在处理客户端消息时抛出的错误, 准备错误消息或阻止发送错误消息.
	 * <p>请注意, STOMP协议要求服务器在发送ERROR帧后关闭连接.
	 * 为了防止发送ERROR帧, 处理器可以返回{@code null}并通过代理发送通知消息, e.g. 通过用户目标.
	 * 
	 * @param clientMessage 与错误相关的客户端消息, 如果在解析WebSocket消息时发生错误, 可能是{@code null}
	 * @param ex 错误的原因, never {@code null}
	 * 
	 * @return 要发送给客户端的错误消息, 或{@code null}在这种情况下不会发送任何消息
	 */
	Message<P> handleClientMessageProcessingError(Message<P> clientMessage, Throwable ex);

	/**
	 * 处理从服务器端发送到客户端的错误, e.g. 来自
	 * {@link org.springframework.messaging.simp.stomp.StompBrokerRelayMessageHandler "broke relay"}的错误,
	 * 因为连接失败或外部代理发送了错误消息等.
	 * 
	 * @param errorMessage 错误消息, never {@code null}
	 * 
	 * @return 要发送给客户端的错误消息, 或{@code null}在这种情况下不会发送任何消息
	 */
	Message<P> handleErrorMessageToClient(Message<P> errorMessage);

}
