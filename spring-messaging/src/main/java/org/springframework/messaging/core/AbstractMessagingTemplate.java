package org.springframework.messaging.core;

import java.util.Map;

import org.springframework.messaging.Message;

/**
 * {@link AbstractMessageReceivingTemplate}的扩展,
 * 增加了对{@link MessageRequestReplyOperations}定义的请求-回复样式操作的支持.
 */
public abstract class AbstractMessagingTemplate<D> extends AbstractMessageReceivingTemplate<D>
		implements MessageRequestReplyOperations<D> {

	@Override
	public Message<?> sendAndReceive(Message<?> requestMessage) {
		return sendAndReceive(getRequiredDefaultDestination(), requestMessage);
	}

	@Override
	public Message<?> sendAndReceive(D destination, Message<?> requestMessage) {
		return doSendAndReceive(destination, requestMessage);
	}

	protected abstract Message<?> doSendAndReceive(D destination, Message<?> requestMessage);


	@Override
	public <T> T convertSendAndReceive(Object request, Class<T> targetClass) {
		return convertSendAndReceive(getRequiredDefaultDestination(), request, targetClass);
	}

	@Override
	public <T> T convertSendAndReceive(D destination, Object request, Class<T> targetClass) {
		return convertSendAndReceive(destination, request, null, targetClass);
	}

	@Override
	public <T> T convertSendAndReceive(D destination, Object request, Map<String, Object> headers, Class<T> targetClass) {
		return convertSendAndReceive(destination, request, headers, targetClass, null);
	}

	@Override
	public <T> T convertSendAndReceive(Object request, Class<T> targetClass, MessagePostProcessor postProcessor) {
		return convertSendAndReceive(getRequiredDefaultDestination(), request, targetClass, postProcessor);
	}

	@Override
	public <T> T convertSendAndReceive(D destination, Object request, Class<T> targetClass, MessagePostProcessor postProcessor) {
		return convertSendAndReceive(destination, request, null, targetClass, postProcessor);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T convertSendAndReceive(D destination, Object request, Map<String, Object> headers,
			Class<T> targetClass, MessagePostProcessor postProcessor) {

		Message<?> requestMessage = doConvert(request, headers, postProcessor);
		Message<?> replyMessage = sendAndReceive(destination, requestMessage);
		return (replyMessage != null ? (T) getMessageConverter().fromMessage(replyMessage, targetClass) : null);
	}

}
