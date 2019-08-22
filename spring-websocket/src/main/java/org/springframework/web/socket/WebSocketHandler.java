package org.springframework.web.socket;

/**
 * WebSocket消息和生命周期事件的处理器.
 *
 * <p>鼓励此接口的实现在本地处理异常, 或者让异常冒泡, 在这种情况下, 默认会记录异常,
 * 并使用 {@link CloseStatus#SERVER_ERROR SERVER_ERROR(1011)}关闭会话.
 * 异常处理策略由
 * {@link org.springframework.web.socket.handler.ExceptionWebSocketHandlerDecorator
 * ExceptionWebSocketHandlerDecorator}提供, 可以通过使用不同的装饰器装饰{@link WebSocketHandler}来定制或替换它.
 */
public interface WebSocketHandler {

	/**
	 * 在WebSocket协商成功并且WebSocket连接打开并准备好使用后调用.
	 * 
	 * @throws Exception 此方法可以处理或传播异常; 有关详细信息, 请参阅类级Javadoc.
	 */
	void afterConnectionEstablished(WebSocketSession session) throws Exception;

	/**
	 * 在新的WebSocket消息到达时调用.
	 * 
	 * @throws Exception 此方法可以处理或传播异常; 有关详细信息, 请参阅类级Javadoc.
	 */
	void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception;

	/**
	 * 处理底层WebSocket消息传输中的错误.
	 * 
	 * @throws Exception 此方法可以处理或传播异常; 有关详细信息, 请参阅类级Javadoc.
	 */
	void handleTransportError(WebSocketSession session, Throwable exception) throws Exception;

	/**
	 * WebSocket连接被任何一方关闭后, 或者在发生传输错误后调用.
	 * 虽然会话在技术上可能仍然是打开的, 但取决于底层实现, 此时不鼓励发送消息, 并且很可能不会成功.
	 * 
	 * @throws Exception 此方法可以处理或传播异常; 有关详细信息, 请参阅类级Javadoc.
	 */
	void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception;

	/**
	 * WebSocketHandler是否处理部分消息.
	 * 如果此标志设置为{@code true}且底层WebSocket服务器支持部分消息, 则可能会拆分大型WebSocket消息或未知大小的消息,
	 * 并且可能通过多次调用{@link #handleMessage(WebSocketSession, WebSocketMessage)}接收.
	 * 标志{@link org.springframework.web.socket.WebSocketMessage#isLast()指示消息是否是部分消息以及它是否是最后一部分.
	 */
	boolean supportsPartialMessages();

}
