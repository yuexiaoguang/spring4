package org.springframework.jms.listener.adapter;

import javax.jms.JMSException;
import javax.jms.Session;

import org.springframework.core.MethodParameter;
import org.springframework.jms.support.JmsHeaderMapper;
import org.springframework.jms.support.converter.MessageConversionException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.core.AbstractMessageSendingTemplate;
import org.springframework.messaging.handler.invocation.InvocableHandlerMethod;
import org.springframework.messaging.support.MessageBuilder;

/**
 * {@link javax.jms.MessageListener}适配器, 调用可配置的{@link InvocableHandlerMethod}.
 *
 * <p>将传入的{@link javax.jms.Message}包装到Spring的{@link Message}抽象中,
 * 使用可配置的{@link JmsHeaderMapper}复制JMS标准header.
 *
 * <p>原始的{@link javax.jms.Message}和{@link javax.jms.Session}作为附加参数提供,
 * 以便在必要时将它们作为方法参数注入.
 */
public class MessagingMessageListenerAdapter extends AbstractAdaptableMessageListener {

	private InvocableHandlerMethod handlerMethod;


	/**
	 * 设置{@link InvocableHandlerMethod}, 用于调用方法处理传入的{@link javax.jms.Message}.
	 */
	public void setHandlerMethod(InvocableHandlerMethod handlerMethod) {
		this.handlerMethod = handlerMethod;
	}


	@Override
	public void onMessage(javax.jms.Message jmsMessage, Session session) throws JMSException {
		Message<?> message = toMessagingMessage(jmsMessage);
		if (logger.isDebugEnabled()) {
			logger.debug("Processing [" + message + "]");
		}
		Object result = invokeHandler(jmsMessage, session, message);
		if (result != null) {
			handleResult(result, jmsMessage, session);
		}
		else {
			logger.trace("No result object given - no result to handle");
		}
	}

	@Override
	protected Object preProcessResponse(Object result) {
		MethodParameter returnType = this.handlerMethod.getReturnType();
		if (result instanceof Message) {
			return MessageBuilder.fromMessage((Message<?>) result)
					.setHeader(AbstractMessageSendingTemplate.CONVERSION_HINT_HEADER, returnType).build();
		}
		return MessageBuilder.withPayload(result).setHeader(
				AbstractMessageSendingTemplate.CONVERSION_HINT_HEADER, returnType).build();
	}

	protected Message<?> toMessagingMessage(javax.jms.Message jmsMessage) {
		try {
			return (Message<?>) getMessagingMessageConverter().fromMessage(jmsMessage);
		}
		catch (JMSException ex) {
			throw new MessageConversionException("Could not convert JMS message", ex);
		}
	}

	/**
	 * 调用处理器, 使用专用错误消息将任何异常包装到{@link ListenerExecutionFailedException}.
	 */
	private Object invokeHandler(javax.jms.Message jmsMessage, Session session, Message<?> message) {
		try {
			return this.handlerMethod.invoke(message, jmsMessage, session);
		}
		catch (MessagingException ex) {
			throw new ListenerExecutionFailedException(
					createMessagingErrorMessage("Listener method could not be invoked with incoming message"), ex);
		}
		catch (Exception ex) {
			throw new ListenerExecutionFailedException("Listener method '" +
					this.handlerMethod.getMethod().toGenericString() + "' threw exception", ex);
		}
	}

	private String createMessagingErrorMessage(String description) {
		StringBuilder sb = new StringBuilder(description).append("\n")
				.append("Endpoint handler details:\n")
				.append("Method [").append(this.handlerMethod.getMethod()).append("]\n")
				.append("Bean [").append(this.handlerMethod.getBean()).append("]\n");
		return sb.toString();
	}
}
