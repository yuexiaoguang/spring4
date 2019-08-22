package org.springframework.web.socket;

import java.nio.ByteBuffer;

/**
 * 二进制WebSocket消息.
 */
public final class BinaryMessage extends AbstractWebSocketMessage<ByteBuffer> {

	/**
	 * @param payload 非null有效负载
	 */
	public BinaryMessage(ByteBuffer payload) {
		super(payload, true);
	}

	/**
	 * 创建一个新的二进制WebSocket消息, 其中给定的有效负载表示完整或部分消息内容.
	 * 当{@code isLast} boolean标志设置为{@code false}时, 消息将作为部分内容发送,
	 * 并且在设置为{@code true}之前, 将会出现更多部分消息.
	 * 
	 * @param payload 非null有效负载
	 * @param isLast 消息是否是一系列部分消息中的最后一个消息
	 */
	public BinaryMessage(ByteBuffer payload, boolean isLast) {
		super(payload, isLast);
	}

	/**
	 * @param payload 非null有效负载; 请注意, 不会复制此值, 因此必须小心不要修改数组.
	 */
	public BinaryMessage(byte[] payload) {
		this(payload, true);
	}

	/**
	 * 使用给定的 byte[]有效负载创建一个新的二进制WebSocket消息, 该有效负载表示完整或部分消息内容.
	 * 当{@code isLast} boolean标志设置为{@code false}时, 消息将作为部分内容发送,
	 * 并且在设置为{@code true}之前, 将会出现更多部分消息.
	 * 
	 * @param payload 非null有效负载; 请注意, 不会复制此值, 因此必须小心不要修改数组.
	 * @param isLast 消息是否是一系列部分消息中的最后一个消息
	 */
	public BinaryMessage(byte[] payload, boolean isLast) {
		this(payload, 0, ((payload == null) ? 0 : payload.length), isLast);
	}

	/**
	 * 通过包装现有的字节数组来创建新的二进制WebSocket消息.
	 * 
	 * @param payload 非null有效负载; 请注意, 不会复制此值, 因此必须小心不要修改数组.
	 * @param offset 有效载荷开始的数组的偏移量
	 * @param length 有效载荷的长度
	 * @param isLast 消息是否是一系列部分消息中的最后一个消息
	 */
	public BinaryMessage(byte[] payload, int offset, int length, boolean isLast) {
		super(payload != null ? ByteBuffer.wrap(payload, offset, length) : null, isLast);
	}


	@Override
	public int getPayloadLength() {
		return getPayload().remaining();
	}

	@Override
	protected String toStringPayload() {
		return getPayload().toString();
	}

}
