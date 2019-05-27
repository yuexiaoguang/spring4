package org.springframework.messaging.support;

import java.io.Serializable;
import java.util.Map;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * 带有通用有效负载的{@link Message}的实现.
 * 一旦创建, GenericMessage就是不可变的.
 */
public class GenericMessage<T> implements Message<T>, Serializable {

	private static final long serialVersionUID = 4268801052358035098L;


	private final T payload;

	private final MessageHeaders headers;


	/**
	 * @param payload 消息有效负载 (never {@code null})
	 */
	public GenericMessage(T payload) {
		this(payload, new MessageHeaders(null));
	}

	/**
	 * 复制给定header Map的内容.
	 * 
	 * @param payload 消息有效负载 (never {@code null})
	 * @param headers 用于初始化的消息 header
	 */
	public GenericMessage(T payload, Map<String, Object> headers) {
		this(payload, new MessageHeaders(headers));
	}

	/**
	 * <p><strong>Note:</strong> 给定的{@code MessageHeaders}实例直接在新消息中使用, i.e. 不复制它.
	 * 
	 * @param payload 消息有效负载 (never {@code null})
	 * @param headers 消息header
	 */
	public GenericMessage(T payload, MessageHeaders headers) {
		Assert.notNull(payload, "Payload must not be null");
		Assert.notNull(headers, "MessageHeaders must not be null");
		this.payload = payload;
		this.headers = headers;
	}


	public T getPayload() {
		return this.payload;
	}

	public MessageHeaders getHeaders() {
		return this.headers;
	}


	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof GenericMessage)) {
			return false;
		}
		GenericMessage<?> otherMsg = (GenericMessage<?>) other;
		// 使用nullSafeEquals进行正确的数组等于比较
		return (ObjectUtils.nullSafeEquals(this.payload, otherMsg.payload) && this.headers.equals(otherMsg.headers));
	}

	public int hashCode() {
		// 使用nullSafeHashCode进行正确的数组hashCode处理
		return (ObjectUtils.nullSafeHashCode(this.payload) * 23 + this.headers.hashCode());
	}

	public String toString() {
		StringBuilder sb = new StringBuilder(getClass().getSimpleName());
		sb.append(" [payload=");
		if (this.payload instanceof byte[]) {
			sb.append("byte[").append(((byte[]) this.payload).length).append("]");
		}
		else {
			sb.append(this.payload);
		}
		sb.append(", headers=").append(this.headers).append("]");
		return sb.toString();
	}

}
