package org.springframework.messaging.simp.stomp;

import java.lang.reflect.Type;

/**
 * {@link StompSessionHandler}的抽象适配器类, 除了{@link #getPayloadType}之外, 大部分都是空的实现方法,
 * 它返回String作为STOMP ERROR帧有效负载所需的默认对象类型.
 */
public abstract class StompSessionHandlerAdapter implements StompSessionHandler {

	/**
	 * This implementation is empty.
	 */
	@Override
	public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
	}

	/**
	 * 此实现返回String作为STOMP ERROR帧的预期有效内容类型.
	 */
	@Override
	public Type getPayloadType(StompHeaders headers) {
		return String.class;
	}

	/**
	 * This implementation is empty.
	 */
	@Override
	public void handleFrame(StompHeaders headers, Object payload) {
	}

	/**
	 * This implementation is empty.
	 */
	@Override
	public void handleException(StompSession session, StompCommand command, StompHeaders headers,
			byte[] payload, Throwable exception) {
	}

	/**
	 * This implementation is empty.
	 */
	@Override
	public void handleTransportError(StompSession session, Throwable exception) {
	}

}
