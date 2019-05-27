package org.springframework.jms.support;

import java.util.List;
import java.util.Map;
import javax.jms.Destination;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.NativeMessageHeaderAccessor;

/**
 * {@link org.springframework.messaging.support.MessageHeaderAccessor}实现, 可以访问特定于JMS的header.
 */
public class JmsMessageHeaderAccessor extends NativeMessageHeaderAccessor {

	protected JmsMessageHeaderAccessor(Map<String, List<String>> nativeHeaders) {
		super(nativeHeaders);
	}

	protected JmsMessageHeaderAccessor(Message<?> message) {
		super(message);
	}


	/**
	 * 返回{@link JmsHeaders#CORRELATION_ID correlationId}.
	 */
	public String getCorrelationId() {
		return (String) getHeader(JmsHeaders.CORRELATION_ID);
	}

	/**
	 * 返回{@link JmsHeaders#DESTINATION 目标}.
	 */
	public Destination getDestination() {
		return (Destination) getHeader(JmsHeaders.DESTINATION);
	}

	/**
	 * 返回{@link JmsHeaders#DELIVERY_MODE 传递模式}.
	 */
	public Integer getDeliveryMode() {
		return (Integer) getHeader(JmsHeaders.DELIVERY_MODE);
	}

	/**
	 * 返回消息{@link JmsHeaders#EXPIRATION 过期}.
	 */
	public Long getExpiration() {
		return (Long) getHeader(JmsHeaders.EXPIRATION);
	}

	/**
	 * 返回{@link JmsHeaders#MESSAGE_ID 消息id}.
	 */
	public String getMessageId() {
		return (String) getHeader(JmsHeaders.MESSAGE_ID);
	}

	/**
	 * 返回{@link JmsHeaders#PRIORITY 优先级}.
	 */
	public Integer getPriority() {
		return (Integer) getHeader(JmsHeaders.PRIORITY);
	}

	/**
	 * 返回{@link JmsHeaders#REPLY_TO 回复目标}.
	 */
	public Destination getReplyTo() {
		return (Destination) getHeader(JmsHeaders.REPLY_TO);
	}

	/**
	 * 返回{@link JmsHeaders#REDELIVERED 重发}标志.
	 */
	public Boolean getRedelivered() {
		return (Boolean) getHeader(JmsHeaders.REDELIVERED);
	}

	/**
	 * 返回{@link JmsHeaders#TYPE 类型}.
	 */
	public String getType() {
		return (String) getHeader(JmsHeaders.TYPE);
	}

	/**
	 * 返回{@link JmsHeaders#TIMESTAMP 时间戳}.
	 */
	@Override
	public Long getTimestamp() {
		return (Long) getHeader(JmsHeaders.TIMESTAMP);
	}


	// Static factory method

	/**
	 * 从现有消息的header创建{@link JmsMessageHeaderAccessor}.
	 */
	public static JmsMessageHeaderAccessor wrap(Message<?> message) {
		return new JmsMessageHeaderAccessor(message);
	}

}
