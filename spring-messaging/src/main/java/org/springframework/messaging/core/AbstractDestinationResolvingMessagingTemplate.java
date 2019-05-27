package org.springframework.messaging.core;

import java.util.Map;

import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * {@link AbstractMessagingTemplate}的扩展, 它添加了将消息发送到可解析目标名称的操作, 如以下接口所定义:
 * <ul>
 * <li>{@link DestinationResolvingMessageSendingOperations}</li>
 * <li>{@link DestinationResolvingMessageReceivingOperations}</li>
 * <li>{@link DestinationResolvingMessageRequestReplyOperations}</li>
 * </ul>
 */
public abstract class AbstractDestinationResolvingMessagingTemplate<D> extends AbstractMessagingTemplate<D>
		implements DestinationResolvingMessageSendingOperations<D>,
		DestinationResolvingMessageReceivingOperations<D>,
		DestinationResolvingMessageRequestReplyOperations<D> {

	private volatile DestinationResolver<D> destinationResolver;


	/**
	 * 配置{@link DestinationResolver}以用于将字符串目标名称解析为{@code <D>}类型的实际目标.
	 * <p>该字段没有默认设置. 如果未配置, 则需要解析目标名称的方法将引发{@link IllegalArgumentException}.
	 * 
	 * @param destinationResolver 要使用的目标解析器
	 */
	public void setDestinationResolver(DestinationResolver<D> destinationResolver) {
		Assert.notNull(destinationResolver, "'destinationResolver' is required");
		this.destinationResolver = destinationResolver;
	}

	/**
	 * 返回配置的目标解析器.
	 */
	public DestinationResolver<D> getDestinationResolver() {
		return this.destinationResolver;
	}


	@Override
	public void send(String destinationName, Message<?> message) {
		D destination = resolveDestination(destinationName);
		doSend(destination, message);
	}

	protected final D resolveDestination(String destinationName) {
		Assert.state(this.destinationResolver != null, "DestinationResolver is required to resolve destination names");
		return this.destinationResolver.resolveDestination(destinationName);
	}

	@Override
	public <T> void convertAndSend(String destinationName, T payload) {
		convertAndSend(destinationName, payload, null, null);
	}

	@Override
	public <T> void convertAndSend(String destinationName, T payload, Map<String, Object> headers) {
		convertAndSend(destinationName, payload, headers, null);
	}

	@Override
	public <T> void convertAndSend(String destinationName, T payload, MessagePostProcessor postProcessor) {
		convertAndSend(destinationName, payload, null, postProcessor);
	}

	@Override
	public <T> void convertAndSend(String destinationName, T payload, Map<String, Object> headers, MessagePostProcessor postProcessor) {
		D destination = resolveDestination(destinationName);
		super.convertAndSend(destination, payload, headers, postProcessor);
	}

	@Override
	public Message<?> receive(String destinationName) {
		D destination = resolveDestination(destinationName);
		return super.receive(destination);
	}

	@Override
	public <T> T receiveAndConvert(String destinationName, Class<T> targetClass) {
		D destination = resolveDestination(destinationName);
		return super.receiveAndConvert(destination, targetClass);
	}

	@Override
	public Message<?> sendAndReceive(String destinationName, Message<?> requestMessage) {
		D destination = resolveDestination(destinationName);
		return super.sendAndReceive(destination, requestMessage);
	}

	@Override
	public <T> T convertSendAndReceive(String destinationName, Object request, Class<T> targetClass) {
		D destination = resolveDestination(destinationName);
		return super.convertSendAndReceive(destination, request, targetClass);
	}

	@Override
	public <T> T convertSendAndReceive(String destinationName, Object request, Map<String, Object> headers,
			Class<T> targetClass) {

		D destination = resolveDestination(destinationName);
		return super.convertSendAndReceive(destination, request, headers, targetClass);
	}

	@Override
	public <T> T convertSendAndReceive(String destinationName, Object request, Class<T> targetClass,
			MessagePostProcessor postProcessor) {

		D destination = resolveDestination(destinationName);
		return super.convertSendAndReceive(destination, request, targetClass, postProcessor);
	}

	@Override
	public <T> T convertSendAndReceive(String destinationName, Object request, Map<String, Object> headers,
			Class<T> targetClass, MessagePostProcessor postProcessor) {

		D destination = resolveDestination(destinationName);
		return super.convertSendAndReceive(destination, request, headers, targetClass, postProcessor);
	}

}
