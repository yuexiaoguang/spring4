package org.springframework.messaging.core;

import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.converter.MessageConverter;

/**
 * {@link AbstractMessageSendingTemplate}的扩展, 它增加了对{@link MessageReceivingOperations}定义的接收样式操作的支持.
 */
public abstract class AbstractMessageReceivingTemplate<D> extends AbstractMessageSendingTemplate<D>
		implements MessageReceivingOperations<D> {

	@Override
	public Message<?> receive() {
		return doReceive(getRequiredDefaultDestination());
	}

	@Override
	public Message<?> receive(D destination) {
		return doReceive(destination);
	}

	/**
	 * 实际从给定目标接收消息.
	 * 
	 * @param destination 目标
	 * 
	 * @return 收到的消息, 或{@code null} 如果无法接收消息, 例如由于超时
	 */
	protected abstract Message<?> doReceive(D destination);


	@Override
	public <T> T receiveAndConvert(Class<T> targetClass) {
		return receiveAndConvert(getRequiredDefaultDestination(), targetClass);
	}

	@Override
	public <T> T receiveAndConvert(D destination, Class<T> targetClass) {
		Message<?> message = doReceive(destination);
		if (message != null) {
			return doConvert(message, targetClass);
		}
		else {
			return null;
		}
	}

	/**
	 * 从给定消息转换为给定目标类.
	 * 
	 * @param message 要转换的消息
	 * @param targetClass 要将有效负载转换为的目标类
	 * 
	 * @return 回复消息转换后的有效负载 (never {@code null})
	 */
	@SuppressWarnings("unchecked")
	protected <T> T doConvert(Message<?> message, Class<T> targetClass) {
		MessageConverter messageConverter = getMessageConverter();
		T value = (T) messageConverter.fromMessage(message, targetClass);
		if (value == null) {
			throw new MessageConversionException(message, "Unable to convert payload [" + message.getPayload() +
					"] to type [" + targetClass + "] using converter [" + messageConverter + "]");
		}
		return value;
	}

}
