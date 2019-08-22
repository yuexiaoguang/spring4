package org.springframework.web.socket;

/**
 * 可以在WebSocket连接上处理或发送的消息.
 */
public interface WebSocketMessage<T> {

	/**
	 * 返回消息有效负载 (never {@code null}).
	 */
	T getPayload();

	/**
	 * 返回消息中包含的字节数.
	 */
	int getPayloadLength();

	/**
	 * 当部分消息支持可用并通过
	 * {@link org.springframework.web.socket.WebSocketHandler#supportsPartialMessages()}请求时,
	 * 如果当前消息是客户端发送的完整WebSocket消息的最后一部分, 则此方法返回{@code true}.
	 * 否则, 如果部分消息支持不可用或未启用, 则返回{@code false}.
	 */
	boolean isLast();

}
