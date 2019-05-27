package org.springframework.messaging.core;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.SimpleMessageConverter;
import org.springframework.messaging.converter.SmartMessageConverter;
import org.springframework.util.Assert;

/**
 * {@link MessageSendingOperations}实现的抽象基类.
 */
public abstract class AbstractMessageSendingTemplate<D> implements MessageSendingOperations<D> {

	/**
	 * 可以设置的header名称, 用于提供有关负载来源的进一步信息 (e.g. {@code MethodParameter}实例),
	 * 作为转换提示考虑在内.
	 */
	public static final String CONVERSION_HINT_HEADER = "conversionHint";


	protected final Log logger = LogFactory.getLog(getClass());

	private volatile D defaultDestination;

	private volatile MessageConverter converter = new SimpleMessageConverter();


	/**
	 * 配置要在没有目标参数的send方法中使用的默认目标.
	 * 如果未配置默认目标, 则不带目标参数的send方法将在调用时引发异常.
	 */
	public void setDefaultDestination(D defaultDestination) {
		this.defaultDestination = defaultDestination;
	}

	/**
	 * 返回配置的默认目标.
	 */
	public D getDefaultDestination() {
		return this.defaultDestination;
	}

	/**
	 * 设置在{@code convertAndSend}方法中使用的{@link MessageConverter}.
	 * <p>默认使用{@link SimpleMessageConverter}.
	 * 
	 * @param messageConverter 要使用的消息转换器
	 */
	public void setMessageConverter(MessageConverter messageConverter) {
		Assert.notNull(messageConverter, "MessageConverter must not be null");
		this.converter = messageConverter;
	}

	/**
	 * 返回配置的{@link MessageConverter}.
	 */
	public MessageConverter getMessageConverter() {
		return this.converter;
	}


	@Override
	public void send(Message<?> message) {
		send(getRequiredDefaultDestination(), message);
	}

	protected final D getRequiredDefaultDestination() {
		Assert.state(this.defaultDestination != null, "No 'defaultDestination' configured");
		return this.defaultDestination;
	}

	@Override
	public void send(D destination, Message<?> message) {
		doSend(destination, message);
	}

	protected abstract void doSend(D destination, Message<?> message);


	@Override
	public void convertAndSend(Object payload) throws MessagingException {
		convertAndSend(payload, null);
	}

	@Override
	public void convertAndSend(D destination, Object payload) throws MessagingException {
		convertAndSend(destination, payload, (Map<String, Object>) null);
	}

	@Override
	public void convertAndSend(D destination, Object payload, Map<String, Object> headers) throws MessagingException {
		convertAndSend(destination, payload, headers, null);
	}

	@Override
	public void convertAndSend(Object payload, MessagePostProcessor postProcessor) throws MessagingException {
		convertAndSend(getRequiredDefaultDestination(), payload, postProcessor);
	}

	@Override
	public void convertAndSend(D destination, Object payload, MessagePostProcessor postProcessor)
			throws MessagingException {

		convertAndSend(destination, payload, null, postProcessor);
	}

	@Override
	public void convertAndSend(D destination, Object payload, Map<String, Object> headers,
			MessagePostProcessor postProcessor) throws MessagingException {

		Message<?> message = doConvert(payload, headers, postProcessor);
		send(destination, message);
	}

	/**
	 * 将给定的Object转换为序列化形式, 可能使用{@link MessageConverter},
	 * 将其包装为带有给定header的消息并应用给定的后处理器.
	 * 
	 * @param payload 要用作有效负载的Object
	 * @param headers 要发送的消息的header
	 * @param postProcessor 要应用于消息的后处理器
	 * 
	 * @return 转换后的消息
	 */
	protected Message<?> doConvert(Object payload, Map<String, Object> headers, MessagePostProcessor postProcessor) {
		MessageHeaders messageHeaders = null;
		Object conversionHint = (headers != null ? headers.get(CONVERSION_HINT_HEADER) : null);

		Map<String, Object> headersToUse = processHeadersToSend(headers);
		if (headersToUse != null) {
			if (headersToUse instanceof MessageHeaders) {
				messageHeaders = (MessageHeaders) headersToUse;
			}
			else {
				messageHeaders = new MessageHeaders(headersToUse);
			}
		}

		MessageConverter converter = getMessageConverter();
		Message<?> message = (converter instanceof SmartMessageConverter ?
				((SmartMessageConverter) converter).toMessage(payload, messageHeaders, conversionHint) :
				converter.toMessage(payload, messageHeaders));
		if (message == null) {
			String payloadType = (payload != null ? payload.getClass().getName() : null);
			Object contentType = (messageHeaders != null ? messageHeaders.get(MessageHeaders.CONTENT_TYPE) : null);
			throw new MessageConversionException("Unable to convert payload with type='" + payloadType +
					"', contentType='" + contentType + "', converter=[" + getMessageConverter() + "]");
		}
		if (postProcessor != null) {
			message = postProcessor.postProcessMessage(message);
		}
		return message;
	}

	/**
	 * 在发送操作之前提供对输入header映射的访问.
	 * 子类可以修改header, 然后返回相同或不同的映射.
	 * <p>此类中的此默认实现返回输入映射.
	 * 
	 * @param headers 要发送的header (或{@code null})
	 * 
	 * @return 要发送的实际header (或{@code null})
	 */
	protected Map<String, Object> processHeadersToSend(Map<String, Object> headers) {
		return headers;
	}

}
