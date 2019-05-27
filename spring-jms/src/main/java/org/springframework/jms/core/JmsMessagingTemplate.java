package org.springframework.jms.core;

import java.util.Map;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Session;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.jms.InvalidDestinationException;
import org.springframework.jms.JmsException;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessagingMessageConverter;
import org.springframework.jms.support.converter.SimpleMessageConverter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.core.AbstractMessagingTemplate;
import org.springframework.messaging.core.DestinationResolutionException;
import org.springframework.messaging.core.MessagePostProcessor;
import org.springframework.util.Assert;

/**
 * {@link JmsMessageOperations}的实现.
 */
public class JmsMessagingTemplate extends AbstractMessagingTemplate<Destination>
		implements JmsMessageOperations, InitializingBean {

	private JmsTemplate jmsTemplate;

	private MessageConverter jmsMessageConverter = new MessagingMessageConverter();

	private boolean converterSet;

	private String defaultDestinationName;


	/**
	 * 需要调用{@link #setConnectionFactory} 或 {@link #setJmsTemplate}.
	 */
	public JmsMessagingTemplate() {
	}

	/**
	 * 隐式构建{@link JmsTemplate}.
	 */
	public JmsMessagingTemplate(ConnectionFactory connectionFactory) {
		this.jmsTemplate = new JmsTemplate(connectionFactory);
	}

	public JmsMessagingTemplate(JmsTemplate jmsTemplate) {
		Assert.notNull(jmsTemplate, "JmsTemplate must not be null");
		this.jmsTemplate = jmsTemplate;
	}


	/**
	 * 设置用于底层{@link JmsTemplate}的ConnectionFactory.
	 */
	public void setConnectionFactory(ConnectionFactory connectionFactory) {
		if (this.jmsTemplate != null) {
			this.jmsTemplate.setConnectionFactory(connectionFactory);
		}
		else {
			this.jmsTemplate = new JmsTemplate(connectionFactory);
		}
	}

	/**
	 * 返回底层{@link JmsTemplate}使用的ConnectionFactory.
	 */
	public ConnectionFactory getConnectionFactory() {
		return (this.jmsTemplate != null ? this.jmsTemplate.getConnectionFactory() : null);
	}

	/**
	 * 设置要使用的{@link JmsTemplate}.
	 */
	public void setJmsTemplate(JmsTemplate jmsTemplate) {
		this.jmsTemplate = jmsTemplate;
	}

	/**
	 * 返回配置的{@link JmsTemplate}.
	 */
	public JmsTemplate getJmsTemplate() {
		return this.jmsTemplate;
	}

	/**
	 * 设置{@link MessageConverter}, 用于转换{@link Message}.
	 * 默认情况下, 使用{@link SimpleMessageConverter}定义{@link MessagingMessageConverter}以转换消息的有效负载.
	 * <p>考虑使用不同的{@link MessagingMessageConverter#setPayloadConverter(MessageConverter) 有效负载转换器}
	 * 配置{@link MessagingMessageConverter}以用于更高级的场景.
	 */
	public void setJmsMessageConverter(MessageConverter jmsMessageConverter) {
		Assert.notNull(jmsMessageConverter, "MessageConverter must not be null");
		this.jmsMessageConverter = jmsMessageConverter;
		this.converterSet = true;
	}

	/**
	 * 返回用于转换{@link Message}的{@link MessageConverter}.
	 */
	public MessageConverter getJmsMessageConverter() {
		return this.jmsMessageConverter;
	}

	/**
	 * 配置要在没有destination参数的send方法中使用的默认目标名称.
	 * 如果未配置默认目标, 则不带destination参数的send方法将在调用时引发异常.
	 */
	public void setDefaultDestinationName(String defaultDestinationName) {
		this.defaultDestinationName = defaultDestinationName;
	}

	/**
	 * 返回配置的默认目标名称.
	 */
	public String getDefaultDestinationName() {
		return this.defaultDestinationName;
	}

	@Override
	public void afterPropertiesSet() {
		Assert.notNull(this.jmsTemplate, "Property 'connectionFactory' or 'jmsTemplate' is required");
		if (!this.converterSet && this.jmsTemplate.getMessageConverter() != null) {
			((MessagingMessageConverter) this.jmsMessageConverter)
					.setPayloadConverter(this.jmsTemplate.getMessageConverter());
		}
	}


	@Override
	public void send(Message<?> message) {
		Destination defaultDestination = getDefaultDestination();
		if (defaultDestination != null) {
			send(defaultDestination, message);
		}
		else {
			send(getRequiredDefaultDestinationName(), message);
		}
	}

	@Override
	public void convertAndSend(Object payload) throws MessagingException {
		convertAndSend(payload, null);
	}

	@Override
	public void convertAndSend(Object payload, MessagePostProcessor postProcessor) throws MessagingException {
		Destination defaultDestination = getDefaultDestination();
		if (defaultDestination != null) {
			convertAndSend(defaultDestination, payload, postProcessor);
		}
		else {
			convertAndSend(getRequiredDefaultDestinationName(), payload, postProcessor);
		}
	}

	@Override
	public void send(String destinationName, Message<?> message) throws MessagingException {
		doSend(destinationName, message);
	}

	@Override
	public void convertAndSend(String destinationName, Object payload) throws MessagingException {
		convertAndSend(destinationName, payload, (Map<String, Object>) null);
	}

	@Override
	public void convertAndSend(String destinationName, Object payload, Map<String, Object> headers)
			throws MessagingException {

		convertAndSend(destinationName, payload, headers, null);
	}

	@Override
	public void convertAndSend(String destinationName, Object payload, MessagePostProcessor postProcessor)
			throws MessagingException {

		convertAndSend(destinationName, payload, null, postProcessor);
	}

	@Override
	public void convertAndSend(String destinationName, Object payload, Map<String, Object> headers,
			MessagePostProcessor postProcessor) throws MessagingException {

		Message<?> message = doConvert(payload, headers, postProcessor);
		send(destinationName, message);
	}

	@Override
	public Message<?> receive() {
		Destination defaultDestination = getDefaultDestination();
		if (defaultDestination != null) {
			return receive(defaultDestination);
		}
		else {
			return receive(getRequiredDefaultDestinationName());
		}
	}

	@Override
	public <T> T receiveAndConvert(Class<T> targetClass) {
		Destination defaultDestination = getDefaultDestination();
		if (defaultDestination != null) {
			return receiveAndConvert(defaultDestination, targetClass);
		}
		else {
			return receiveAndConvert(getRequiredDefaultDestinationName(), targetClass);
		}
	}

	@Override
	public Message<?> receive(String destinationName) throws MessagingException {
		return doReceive(destinationName);
	}

	@Override
	public <T> T receiveAndConvert(String destinationName, Class<T> targetClass) throws MessagingException {
		Message<?> message = doReceive(destinationName);
		if (message != null) {
			return doConvert(message, targetClass);
		}
		else {
			return null;
		}
	}

	@Override
	public Message<?> sendAndReceive(Message<?> requestMessage) {
		Destination defaultDestination = getDefaultDestination();
		if (defaultDestination != null) {
			return sendAndReceive(defaultDestination, requestMessage);
		}
		else {
			return sendAndReceive(getRequiredDefaultDestinationName(), requestMessage);
		}
	}

	@Override
	public Message<?> sendAndReceive(String destinationName, Message<?> requestMessage) throws MessagingException {
		return doSendAndReceive(destinationName, requestMessage);
	}

	@Override
	public <T> T convertSendAndReceive(String destinationName, Object request, Class<T> targetClass)
			throws MessagingException {

		return convertSendAndReceive(destinationName, request, null, targetClass);
	}

	@Override
	public <T> T convertSendAndReceive(Object request, Class<T> targetClass) {
		return convertSendAndReceive(request, targetClass, null);
	}

	@Override
	public <T> T convertSendAndReceive(String destinationName, Object request,
			Map<String, Object> headers, Class<T> targetClass) throws MessagingException {

		return convertSendAndReceive(destinationName, request, headers, targetClass, null);
	}

	@Override
	public <T> T convertSendAndReceive(Object request, Class<T> targetClass, MessagePostProcessor postProcessor) {
		Destination defaultDestination = getDefaultDestination();
		if (defaultDestination != null) {
			return convertSendAndReceive(defaultDestination, request, targetClass, postProcessor);
		}
		else {
			return convertSendAndReceive(getRequiredDefaultDestinationName(), request, targetClass, postProcessor);
		}
	}

	@Override
	public <T> T convertSendAndReceive(String destinationName, Object request, Class<T> targetClass,
			MessagePostProcessor requestPostProcessor) throws MessagingException {

		return convertSendAndReceive(destinationName, request, null, targetClass, requestPostProcessor);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T convertSendAndReceive(String destinationName, Object request, Map<String, Object> headers,
			Class<T> targetClass, MessagePostProcessor postProcessor) {

		Message<?> requestMessage = doConvert(request, headers, postProcessor);
		Message<?> replyMessage = sendAndReceive(destinationName, requestMessage);
		return (replyMessage != null ? (T) getMessageConverter().fromMessage(replyMessage, targetClass) : null);
	}

	@Override
	protected void doSend(Destination destination, Message<?> message) {
		try {
			this.jmsTemplate.send(destination, createMessageCreator(message));
		}
		catch (JmsException ex) {
			throw convertJmsException(ex);
		}
	}

	protected void doSend(String destinationName, Message<?> message) {
		try {
			this.jmsTemplate.send(destinationName, createMessageCreator(message));
		}
		catch (JmsException ex) {
			throw convertJmsException(ex);
		}
	}

	@Override
	protected Message<?> doReceive(Destination destination) {
		try {
			javax.jms.Message jmsMessage = this.jmsTemplate.receive(destination);
			return convertJmsMessage(jmsMessage);
		}
		catch (JmsException ex) {
			throw convertJmsException(ex);
		}
	}

	protected Message<?> doReceive(String destinationName) {
		try {
			javax.jms.Message jmsMessage = this.jmsTemplate.receive(destinationName);
			return convertJmsMessage(jmsMessage);
		}
		catch (JmsException ex) {
			throw convertJmsException(ex);
		}
	}

	@Override
	protected Message<?> doSendAndReceive(Destination destination, Message<?> requestMessage) {
		try {
			javax.jms.Message jmsMessage = this.jmsTemplate.sendAndReceive(
					destination, createMessageCreator(requestMessage));
			return convertJmsMessage(jmsMessage);
		}
		catch (JmsException ex) {
			throw convertJmsException(ex);
		}
	}

	protected Message<?> doSendAndReceive(String destinationName, Message<?> requestMessage) {
		try {
			javax.jms.Message jmsMessage = this.jmsTemplate.sendAndReceive(
					destinationName, createMessageCreator(requestMessage));
			return convertJmsMessage(jmsMessage);
		}
		catch (JmsException ex) {
			throw convertJmsException(ex);
		}
	}

	private MessagingMessageCreator createMessageCreator(Message<?> message) {
		return new MessagingMessageCreator(message, getJmsMessageConverter());
	}

	protected String getRequiredDefaultDestinationName() {
		String name = getDefaultDestinationName();
		if (name == null) {
			throw new IllegalStateException("No 'defaultDestination' or 'defaultDestinationName' specified. " +
					"Check configuration of JmsMessagingTemplate.");
		}
		return name;
	}

	protected Message<?> convertJmsMessage(javax.jms.Message message) {
		if (message == null) {
			return null;
		}
		try {
			return (Message<?>) getJmsMessageConverter().fromMessage(message);
		}
		catch (Exception ex) {
			throw new MessageConversionException("Could not convert '" + message + "'", ex);
		}
	}

	protected MessagingException convertJmsException(JmsException ex) {
		if (ex instanceof org.springframework.jms.support.destination.DestinationResolutionException ||
				ex instanceof InvalidDestinationException) {
			return new DestinationResolutionException(ex.getMessage(), ex);
		}
		if (ex instanceof org.springframework.jms.support.converter.MessageConversionException) {
			return new MessageConversionException(ex.getMessage(), ex);
		}
		// Fallback
		return new MessagingException(ex.getMessage(), ex);
	}


	private static class MessagingMessageCreator implements MessageCreator {

		private final Message<?> message;

		private final MessageConverter messageConverter;

		public MessagingMessageCreator(Message<?> message, MessageConverter messageConverter) {
			this.message = message;
			this.messageConverter = messageConverter;
		}

		@Override
		public javax.jms.Message createMessage(Session session) throws JMSException {
			try {
				return this.messageConverter.toMessage(this.message, session);
			}
			catch (Exception ex) {
				throw new MessageConversionException("Could not convert '" + this.message + "'", ex);
			}
		}
	}

}
