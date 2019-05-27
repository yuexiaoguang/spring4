package org.springframework.messaging.simp.stomp;

/**
 * 客户端STOMP会话生命周期事件的约定, 包括建立会话时的回调, 以及传输或消息处理失败的通知.
 *
 * <p>此约定还扩展了{@link StompFrameHandler}以处理从代理收到的STOMP ERROR帧.
 *
 * <p>此接口的实现应考虑扩展{@link StompSessionHandlerAdapter}.
 */
public interface StompSessionHandler extends StompFrameHandler {

	/**
	 * 在会话准备好使用时调用, i.e. 在连接底层传输 (TCP, WebSocket) 并从代理接收到STOMP CONNECTED帧之后.
	 * 
	 * @param session 客户端STOMP会话
	 * @param connectedHeaders STOMP CONNECTED帧header
	 */
	void afterConnected(StompSession session, StompHeaders connectedHeaders);

	/**
	 * 处理在处理STOMP帧时出现的任何异常, 例如无法转换有效负载或应用程序{@code StompFrameHandler}中未处理的异常.
	 * 
	 * @param session 客户端STOMP会话
	 * @param command 帧的STOMP命令
	 * @param headers the headers
	 * @param payload 原始有效负载
	 * @param exception the exception
	 */
	void handleException(StompSession session, StompCommand command, StompHeaders headers,
			byte[] payload, Throwable exception);

	/**
	 * 处理低级传输错误, 该错误可能是I/O错误或无法编码或解码STOMP消息.
	 * <p>请注意, 当连接丢失而不是通过{@link StompSession#disconnect()}正常关闭时,
	 * {@link org.springframework.messaging.simp.stomp.ConnectionLostException ConnectionLostException}
	 * 将传递给此方法.
	 * 
	 * @param session 客户端STOMP会话
	 * @param exception 发生的异常
	 */
	void handleTransportError(StompSession session, Throwable exception);

}
