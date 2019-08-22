package org.springframework.web.socket;

import java.nio.ByteBuffer;

/**
 * WebSocket pong消息.
 */
public final class PongMessage extends AbstractWebSocketMessage<ByteBuffer> {

	/**
	 * 使用空有效负载创建新的pong消息.
	 */
	public PongMessage() {
		super(ByteBuffer.allocate(0));
	}

	/**
	 * @param payload 非null有效负载
	 */
	public PongMessage(ByteBuffer payload) {
		super(payload);
	}


	@Override
	public int getPayloadLength() {
		return (getPayload() != null ? getPayload().remaining() : 0);
	}

	@Override
	protected String toStringPayload() {
		return (getPayload() != null ? getPayload().toString() : null);
	}

}
