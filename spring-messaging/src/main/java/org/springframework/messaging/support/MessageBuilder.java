package org.springframework.messaging.support;

import java.util.Map;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;

/**
 * 用于创建{@link GenericMessage}的构建器 (如果有效负载类型为{@link Throwable}, 则为{@link ErrorMessage}).
 */
public final class MessageBuilder<T> {

	private final T payload;

	private final Message<T> originalMessage;

	private MessageHeaderAccessor headerAccessor;


	private MessageBuilder(Message<T> originalMessage) {
		Assert.notNull(originalMessage, "Message must not be null");
		this.payload = originalMessage.getPayload();
		this.originalMessage = originalMessage;
		this.headerAccessor = new MessageHeaderAccessor(originalMessage);
	}

	private MessageBuilder(T payload, MessageHeaderAccessor accessor) {
		Assert.notNull(payload, "Payload must not be null");
		Assert.notNull(accessor, "MessageHeaderAccessor must not be null");
		this.payload = payload;
		this.originalMessage = null;
		this.headerAccessor = accessor;
	}


	/**
	 * 通过提供的{@code MessageHeaderAccessor}来设置要使用的消息header.
	 * 
	 * @param accessor 要使用的header
	 */
	public MessageBuilder<T> setHeaders(MessageHeaderAccessor accessor) {
		Assert.notNull(accessor, "MessageHeaderAccessor must not be null");
		this.headerAccessor = accessor;
		return this;
	}

	/**
	 * 设置给定header名称的值.
	 * 如果提供的值为{@code null}, 则将删除header.
	 */
	public MessageBuilder<T> setHeader(String headerName, Object headerValue) {
		this.headerAccessor.setHeader(headerName, headerValue);
		return this;
	}

	/**
	 * 仅当header名称尚未与值关联时, 才设置给定header名称的值.
	 */
	public MessageBuilder<T> setHeaderIfAbsent(String headerName, Object headerValue) {
		this.headerAccessor.setHeaderIfAbsent(headerName, headerValue);
		return this;
	}

	/**
	 * 删除通过'headerPatterns'数组提供的所有header.
	 * 顾名思义, 数组可能包含header名称的简单匹配模式.
	 * 支持的模式样式是: "xxx*", "*xxx", "*xxx*" 和 "xxx*yyy".
	 */
	public MessageBuilder<T> removeHeaders(String... headerPatterns) {
		this.headerAccessor.removeHeaders(headerPatterns);
		return this;
	}
	/**
	 * 删除给定header名称的值.
	 */
	public MessageBuilder<T> removeHeader(String headerName) {
		this.headerAccessor.removeHeader(headerName);
		return this;
	}

	/**
	 * 从提供的Map复制名称-值对.
	 * 此操作将覆盖任何现有值.
	 * 使用 { {@link #copyHeadersIfAbsent(Map)}来避免覆盖值.
	 * 请注意, 'id'和'timestamp' header值永远不会被覆盖.
	 */
	public MessageBuilder<T> copyHeaders(Map<String, ?> headersToCopy) {
		this.headerAccessor.copyHeaders(headersToCopy);
		return this;
	}

	/**
	 * 从提供的Map复制名称-值对.
	 * 此操作将<em>不</em>覆盖任何现有值.
	 */
	public MessageBuilder<T> copyHeadersIfAbsent(Map<String, ?> headersToCopy) {
		this.headerAccessor.copyHeadersIfAbsent(headersToCopy);
		return this;
	}

	public MessageBuilder<T> setReplyChannel(MessageChannel replyChannel) {
		this.headerAccessor.setReplyChannel(replyChannel);
		return this;
	}

	public MessageBuilder<T> setReplyChannelName(String replyChannelName) {
		this.headerAccessor.setReplyChannelName(replyChannelName);
		return this;
	}

	public MessageBuilder<T> setErrorChannel(MessageChannel errorChannel) {
		this.headerAccessor.setErrorChannel(errorChannel);
		return this;
	}

	public MessageBuilder<T> setErrorChannelName(String errorChannelName) {
		this.headerAccessor.setErrorChannelName(errorChannelName);
		return this;
	}

	@SuppressWarnings("unchecked")
	public Message<T> build() {
		if (this.originalMessage != null && !this.headerAccessor.isModified()) {
			return this.originalMessage;
		}
		MessageHeaders headersToUse = this.headerAccessor.toMessageHeaders();
		if (this.payload instanceof Throwable) {
			return (Message<T>) new ErrorMessage((Throwable) this.payload, headersToUse);
		}
		else {
			return new GenericMessage<T>(this.payload, headersToUse);
		}
	}


	/**
	 * 为预先填充了从提供的消息中复制的所有header的新{@link Message}实例创建构建器.
	 * 提供的消息的有效负载也将用作新消息的有效负载.
	 * 
	 * @param message 将从中复制有效负载和所有header的消息
	 */
	public static <T> MessageBuilder<T> fromMessage(Message<T> message) {
		return new MessageBuilder<T>(message);
	}

	/**
	 * 为具有给定有效负载的消息创建新构建器.
	 * 
	 * @param payload 有效负载
	 */
	public static <T> MessageBuilder<T> withPayload(T payload) {
		return new MessageBuilder<T>(payload, new MessageHeaderAccessor());
	}

	/**
	 * 用于创建具有给定有效负载和{@code MessageHeaders}的消息的快捷方式工厂方法.
	 * <p><strong>Note:</strong> 给定的{@code MessageHeaders}实例直接在新消息中使用, i.e. 不复制它.
	 * 
	 * @param payload 要使用的有效负载 (never {@code null})
	 * @param messageHeaders 要使用的header (never {@code null})
	 * 
	 * @return 创建的消息
	 */
	@SuppressWarnings("unchecked")
	public static <T> Message<T> createMessage(T payload, MessageHeaders messageHeaders) {
		Assert.notNull(payload, "Payload must not be null");
		Assert.notNull(messageHeaders, "MessageHeaders must not be null");
		if (payload instanceof Throwable) {
			return (Message<T>) new ErrorMessage((Throwable) payload, messageHeaders);
		}
		else {
			return new GenericMessage<T>(payload, messageHeaders);
		}
	}

}
