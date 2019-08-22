package org.springframework.web.socket;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * 可以在WebSocket连接上处理或发送的消息.
 */
public abstract class AbstractWebSocketMessage<T> implements WebSocketMessage<T> {

	private final T payload;

	private final boolean last;


	/**
	 * @param payload 非null的有效负载
	 */
	AbstractWebSocketMessage(T payload) {
		this(payload, true);
	}

	/**
	 * 给定有效负载表示完整或部分消息内容.
	 * 当{@code isLast} boolean标志设置为{@code false}时, 消息将作为部分内容发送,
	 * 并且在设置为{@code true}之前, 将会出现更多部分消息.
	 * 
	 * @param payload 非null的有效负载
	 * @param isLast 如果消息是一系列部分消息中的最后一个消息
	 */
	AbstractWebSocketMessage(T payload, boolean isLast) {
		Assert.notNull(payload, "payload must not be null");
		this.payload = payload;
		this.last = isLast;
	}


	/**
	 * 返回消息有效负载, 永远不会是{@code null}.
	 */
	public T getPayload() {
		return this.payload;
	}

	/**
	 * 是否是作为一系列部分消息发送的消息的最后部分.
	 */
	public boolean isLast() {
		return this.last;
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof AbstractWebSocketMessage)) {
			return false;
		}
		AbstractWebSocketMessage<?> otherMessage = (AbstractWebSocketMessage<?>) other;
		return ObjectUtils.nullSafeEquals(this.payload, otherMessage.payload);
	}

	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHashCode(this.payload);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " payload=[" + toStringPayload() +
				"], byteCount=" + getPayloadLength() + ", last=" + isLast() + "]";
	}

	protected abstract String toStringPayload();

}
