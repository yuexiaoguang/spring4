package org.springframework.jms.support.converter;

import java.util.Map;
import javax.jms.JMSException;
import javax.jms.Session;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.jms.support.JmsHeaderMapper;
import org.springframework.jms.support.SimpleJmsHeaderMapper;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.core.AbstractMessagingTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;

/**
 * 使用底层{@link MessageConverter}进行{@link Message}和{@link javax.jms.Message}之间的相互转换,
 * 以及{@link org.springframework.jms.support.JmsHeaderMapper}进行JMS header和标准消息header之间的映射.
 */
public class MessagingMessageConverter implements MessageConverter, InitializingBean {

	private MessageConverter payloadConverter;

	private JmsHeaderMapper headerMapper;


	/**
	 * 使用默认的有效负载转换器.
	 */
	public MessagingMessageConverter() {
		this(new SimpleMessageConverter(), new SimpleJmsHeaderMapper());
	}

	/**
	 * @param payloadConverter 要使用的有效负载转换器
	 */
	public MessagingMessageConverter(MessageConverter payloadConverter) {
		this(payloadConverter, new SimpleJmsHeaderMapper());
	}

	/**
	 * 使用指定的有效负载转换器和header映射器创建实例.
	 */
	public MessagingMessageConverter(MessageConverter payloadConverter, JmsHeaderMapper headerMapper) {
		Assert.notNull(payloadConverter, "PayloadConverter must not be null");
		Assert.notNull(headerMapper, "HeaderMapper must not be null");
		this.payloadConverter = payloadConverter;
		this.headerMapper = headerMapper;
	}


	/**
	 * 设置用于转换有效负载的{@link MessageConverter}.
	 */
	public void setPayloadConverter(MessageConverter payloadConverter) {
		this.payloadConverter = payloadConverter;
	}

	/**
	 * 设置{@link JmsHeaderMapper}, 用于在JMS header和标准消息header之间进行映射.
	 */
	public void setHeaderMapper(JmsHeaderMapper headerMapper) {
		this.headerMapper = headerMapper;
	}

	@Override
	public void afterPropertiesSet() {
		Assert.notNull(this.payloadConverter, "Property 'payloadConverter' is required");
		Assert.notNull(this.headerMapper, "Property 'headerMapper' is required");
	}


	@Override
	public javax.jms.Message toMessage(Object object, Session session) throws JMSException, MessageConversionException {
		if (!(object instanceof Message)) {
			throw new IllegalArgumentException("Could not convert [" + object + "] - only [" +
					Message.class.getName() + "] is handled by this converter");
		}
		Message<?> input = (Message<?>) object;
		MessageHeaders headers = input.getHeaders();
		Object conversionHint = (headers != null ? headers.get(
				AbstractMessagingTemplate.CONVERSION_HINT_HEADER) : null);
		javax.jms.Message reply = createMessageForPayload(input.getPayload(), session, conversionHint);
		this.headerMapper.fromHeaders(headers, reply);
		return reply;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object fromMessage(javax.jms.Message message) throws JMSException, MessageConversionException {
		if (message == null) {
			return null;
		}
		Map<String, Object> mappedHeaders = extractHeaders(message);
		Object convertedObject = extractPayload(message);
		MessageBuilder<Object> builder = (convertedObject instanceof org.springframework.messaging.Message) ?
				MessageBuilder.fromMessage((org.springframework.messaging.Message<Object>) convertedObject) :
				MessageBuilder.withPayload(convertedObject);
		return builder.copyHeadersIfAbsent(mappedHeaders).build();
	}

	/**
	 * 提取指定的{@link javax.jms.Message}的有效负载.
	 */
	protected Object extractPayload(javax.jms.Message message) throws JMSException {
		return this.payloadConverter.fromMessage(message);
	}

	/**
	 * 为指定的有效内容创建JMS消息.
	 * 
	 * @deprecated as of 4.3, use {@link #createMessageForPayload(Object, Session, Object)}
	 */
	@Deprecated
	protected javax.jms.Message createMessageForPayload(Object payload, Session session) throws JMSException {
		return this.payloadConverter.toMessage(payload, session);
	}

	/**
	 * 为指定的有效负载和conversionHint创建JMS消息.
	 * 转换提示是传递给{@link MessageConverter}的额外对象, e.g. 相关的{@code MethodParameter} (may be {@code null}}.
	 */
	@SuppressWarnings("deprecation")
	protected javax.jms.Message createMessageForPayload(Object payload, Session session, Object conversionHint)
			throws JMSException {

		return createMessageForPayload(payload, session);
	}

	protected final MessageHeaders extractHeaders(javax.jms.Message message) {
		return this.headerMapper.toHeaders(message);
	}
}
