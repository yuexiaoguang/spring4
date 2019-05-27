package org.springframework.jms.listener.adapter;

import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.InvalidDestinationException;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.jms.listener.SessionAwareMessageListener;
import org.springframework.jms.support.JmsHeaderMapper;
import org.springframework.jms.support.JmsUtils;
import org.springframework.jms.support.SimpleJmsHeaderMapper;
import org.springframework.jms.support.converter.MessageConversionException;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessagingMessageConverter;
import org.springframework.jms.support.converter.SimpleMessageConverter;
import org.springframework.jms.support.converter.SmartMessageConverter;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.jms.support.destination.DynamicDestinationResolver;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;

/**
 * 抽象的JMS {@link MessageListener}适配器, 提供必要的基础结构来提取JMS {@link Message}的有效负载.
 */
public abstract class AbstractAdaptableMessageListener
		implements MessageListener, SessionAwareMessageListener<Message> {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private Object defaultResponseDestination;

	private DestinationResolver destinationResolver = new DynamicDestinationResolver();

	private MessageConverter messageConverter = new SimpleMessageConverter();

	private final MessagingMessageConverterAdapter messagingMessageConverter = new MessagingMessageConverterAdapter();


	/**
	 * 设置发送响应消息的默认目标.
	 * 这将在请求消息不带有"JMSReplyTo"字段的情况下应用.
	 * <p>响应目标仅与返回结果对象的监听器方法相关, 后者将包装在响应消息中并发送到响应目标.
	 * <p>或者, 指定"defaultResponseQueueName" 或 "defaultResponseTopicName", 以通过DestinationResolver动态解析.
	 */
	public void setDefaultResponseDestination(Destination destination) {
		this.defaultResponseDestination = destination;
	}

	/**
	 * 设置要向其发送响应消息的默认响应队列的名称.
	 * 这将在请求消息不带有"JMSReplyTo"字段的情况下应用.
	 * <p>或者, 将JMS Destination对象指定为 "defaultResponseDestination".
	 */
	public void setDefaultResponseQueueName(String destinationName) {
		this.defaultResponseDestination = new DestinationNameHolder(destinationName, false);
	}

	/**
	 * 设置要向其发送响应消息的默认响应主题的名称.
	 * 这将在请求消息不带有"JMSReplyTo"字段的情况下应用.
	 * <p>或者, 将JMS Destination对象指定为 "defaultResponseDestination".
	 */
	public void setDefaultResponseTopicName(String destinationName) {
		this.defaultResponseDestination = new DestinationNameHolder(destinationName, true);
	}

	/**
	 * 设置应该用于解析此适配器的响应目标名称的DestinationResolver.
	 * <p>默认解析器是 DynamicDestinationResolver.
	 * 指定将目标名称解析为JNDI位置的JndiDestinationResolver.
	 */
	public void setDestinationResolver(DestinationResolver destinationResolver) {
		Assert.notNull(destinationResolver, "DestinationResolver must not be null");
		this.destinationResolver = destinationResolver;
	}

	/**
	 * 返回此适配器的DestinationResolver.
	 */
	protected DestinationResolver getDestinationResolver() {
		return this.destinationResolver;
	}

	/**
	 * 设置转换器, 用于将传入的JMS消息转换为监听器方法参数, 以及将监听器方法返回的对象转换回JMS消息.
	 * <p>默认转换器是{@link SimpleMessageConverter}, 它能够处理{@link javax.jms.BytesMessage BytesMessages},
	 * {@link javax.jms.TextMessage TextMessages} 和 {@link javax.jms.ObjectMessage ObjectMessages}.
	 */
	public void setMessageConverter(MessageConverter messageConverter) {
		this.messageConverter = messageConverter;
	}

	/**
	 * 返回转换器, 用于将传入的JMS消息转换为监听器方法参数, 以及将监听器方法返回的对象转换回JMS消息.
	 */
	protected MessageConverter getMessageConverter() {
		return this.messageConverter;
	}

	/**
	 * 设置用于映射标准JMS header的{@link JmsHeaderMapper}实现.
	 * 默认使用{@link SimpleJmsHeaderMapper}.
	 */
	public void setHeaderMapper(JmsHeaderMapper headerMapper) {
		Assert.notNull(headerMapper, "HeaderMapper must not be null");
		this.messagingMessageConverter.setHeaderMapper(headerMapper);
	}

	/**
	 * 返回此监听器的{@link MessagingMessageConverter}, 可以转换{@link org.springframework.messaging.Message}.
	 */
	protected final MessagingMessageConverter getMessagingMessageConverter() {
		return this.messagingMessageConverter;
	}


	/**
	 * 标准JMS {@link MessageListener}入口点.
	 * <p>将消息委托给目标侦听器方法, 并对message参数进行适当的转换.
	 * 如果发生异常, 将调用{@link #handleListenerException(Throwable)}方法.
	 * <p><b>Note:</b> 不支持基于监听器方法返回的结果对象发送响应消息.
	 * 使用{@link SessionAwareMessageListener}入口点 (通常通过Spring消息监听器容器) 来处理结果对象.
	 * 
	 * @param message 传入的JMS消息
	 */
	@Override
	public void onMessage(Message message) {
		try {
			onMessage(message, null);
		}
		catch (Throwable ex) {
			handleListenerException(ex);
		}
	}

	@Override
	public abstract void onMessage(Message message, Session session) throws JMSException;

	/**
	 * 处理在监听器执行期间出现的给定异常.
	 * 默认实现在错误级别记录异常.
	 * <p>此方法仅在用作标准JMS {@link MessageListener}时适用.
	 * 在Spring {@link SessionAwareMessageListener}机制的情况下, 异常由调用者处理.
	 * 
	 * @param ex 要处理的异常
	 */
	protected void handleListenerException(Throwable ex) {
		logger.error("Listener execution failed", ex);
	}


	/**
	 * 从给定的JMS消息中提取消息正文.
	 * 
	 * @param message the JMS {@code Message}
	 * 
	 * @return 消息的内容, 作为参数传递给listener方法
	 * @throws MessageConversionException 如果无法提取消息
	 */
	protected Object extractMessage(Message message)  {
		try {
			MessageConverter converter = getMessageConverter();
			if (converter != null) {
				return converter.fromMessage(message);
			}
			return message;
		}
		catch (JMSException ex) {
			throw new MessageConversionException("Could not convert JMS message", ex);
		}
	}

	/**
	 * 处理从监听器方法返回的给定结果对象, 发回响应消息.
	 * 
	 * @param result 要处理的结果对象 (never {@code null})
	 * @param request 原始请求消息
	 * @param session 要运行的JMS会话 (may be {@code null})
	 * 
	 * @throws ReplyFailureException 如果无法发送响应消息
	 */
	protected void handleResult(Object result, Message request, Session session) {
		if (session != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Listener method returned result [" + result +
						"] - generating response message for it");
			}
			try {
				Message response = buildMessage(session, result);
				postProcessResponse(request, response);
				Destination destination = getResponseDestination(request, response, session, result);
				sendResponse(session, destination, response);
			}
			catch (Exception ex) {
				throw new ReplyFailureException("Failed to send reply with payload [" + result + "]", ex);
			}
		}

		else {
			// No JMS Session available
			if (logger.isWarnEnabled()) {
				logger.warn("Listener method returned result [" + result +
						"]: not generating response message for it because of no JMS Session given");
			}
		}
	}

	/**
	 * 根据给定的结果对象构建要作为响应发送的JMS消息.
	 * 
	 * @param session 要运行的JMS会话
	 * @param result 从监听器方法返回的消息内容
	 * 
	 * @return the JMS {@code Message} (never {@code null})
	 * @throws JMSException 如果由JMS API方法抛出
	 */
	protected Message buildMessage(Session session, Object result) throws JMSException {
		Object content = preProcessResponse(result instanceof JmsResponse
				? ((JmsResponse<?>) result).getResponse() : result);

		MessageConverter converter = getMessageConverter();
		if (converter != null) {
			if (content instanceof org.springframework.messaging.Message) {
				return this.messagingMessageConverter.toMessage(content, session);
			}
			else {
				return converter.toMessage(content, session);
			}
		}

		if (!(content instanceof Message)) {
			throw new MessageConversionException(
					"No MessageConverter specified - cannot handle message [" + content + "]");
		}
		return (Message) content;
	}

	/**
	 * 在将给定结果转换为{@link Message}之前对其进行预处理.
	 * 
	 * @param result 调用的结果
	 * 
	 * @return 要处理的负载响应, {@code result}参数或任何其他对象 (例如包装结果的实例).
	 */
	protected Object preProcessResponse(Object result) {
		return result;
	}

	/**
	 * 在发送之前对给定的响应消息进行后处理.
	 * <p>默认实现将响应的相关ID设置为请求消息的相关ID; 否则为请求消息ID.
	 * 
	 * @param request 原始传入的JMS消息
	 * @param response 即将发送的传出JMS消息
	 * 
	 * @throws JMSException 如果由JMS API方法抛出
	 */
	protected void postProcessResponse(Message request, Message response) throws JMSException {
		String correlation = request.getJMSCorrelationID();
		if (correlation == null) {
			correlation = request.getJMSMessageID();
		}
		response.setJMSCorrelationID(correlation);
	}

	private Destination getResponseDestination(Message request, Message response, Session session, Object result)
			throws JMSException {

		if (result instanceof JmsResponse) {
			JmsResponse<?> jmsResponse = (JmsResponse) result;
			Destination destination = jmsResponse.resolveDestination(getDestinationResolver(), session);
			if (destination != null) {
				return destination;
			}
		}
		return getResponseDestination(request, response, session);
	}

	/**
	 * 确定给定消息的响应目标.
	 * <p>默认实现首先检查所提供请求的JMS Reply-To {@link Destination}; 如果不是{@code null}则返回;
	 * 如果是{@code null}, 则返回配置的{@link #resolveDefaultResponseDestination 默认响应目标};
	 * 如果这也是{@code null}, 那么抛出{@link javax.jms.InvalidDestinationException}.
	 * 
	 * @param request 原始传入的JMS消息
	 * @param response 即将发送的传出JMS消息
	 * @param session 要运行的JMS会话
	 * 
	 * @return 响应目标 (never {@code null})
	 * @throws JMSException 如果由JMS API方法抛出
	 * @throws javax.jms.InvalidDestinationException 如果无法确定{@link Destination}
	 */
	protected Destination getResponseDestination(Message request, Message response, Session session)
			throws JMSException {

		Destination replyTo = request.getJMSReplyTo();
		if (replyTo == null) {
			replyTo = resolveDefaultResponseDestination(session);
			if (replyTo == null) {
				throw new InvalidDestinationException("Cannot determine response destination: " +
						"Request message does not contain reply-to destination, and no default response destination set.");
			}
		}
		return replyTo;
	}

	/**
	 * 将默认响应目标解析为JMS {@link Destination}, 使用此访问者的{@link DestinationResolver}.
	 * 
	 * @return 定位的{@link Destination}
	 * @throws javax.jms.JMSException 如果解析失败
	 */
	protected Destination resolveDefaultResponseDestination(Session session) throws JMSException {
		if (this.defaultResponseDestination instanceof Destination) {
			return (Destination) this.defaultResponseDestination;
		}
		if (this.defaultResponseDestination instanceof DestinationNameHolder) {
			DestinationNameHolder nameHolder = (DestinationNameHolder) this.defaultResponseDestination;
			return getDestinationResolver().resolveDestinationName(session, nameHolder.name, nameHolder.isTopic);
		}
		return null;
	}

	/**
	 * 将给定的响应消息发送到给定目标.
	 * 
	 * @param response 要发送的JMS消息
	 * @param destination 要发送到的JMS目标
	 * @param session 要运行的JMS会话
	 * 
	 * @throws JMSException 如果由JMS API方法抛出
	 */
	protected void sendResponse(Session session, Destination destination, Message response) throws JMSException {
		MessageProducer producer = session.createProducer(destination);
		try {
			postProcessProducer(producer, response);
			producer.send(response);
		}
		finally {
			JmsUtils.closeMessageProducer(producer);
		}
	}

	/**
	 * 在使用它发送响应之前对给定的消息生产者进行后处理.
	 * <p>默认实现为空.
	 * 
	 * @param producer 将用于发送消息的JMS消息生产者
	 * @param response 即将发送的传出JMS消息
	 * 
	 * @throws JMSException 如果由JMS API方法抛出
	 */
	protected void postProcessProducer(MessageProducer producer, Message response) throws JMSException {
	}


	/**
	 * 延迟调用有效负载提取, 并将其委托给 {@link #extractMessage(javax.jms.Message)}, 以强制向后兼容.
	 */
	private class MessagingMessageConverterAdapter extends MessagingMessageConverter {

		@SuppressWarnings("unchecked")
		@Override
		public Object fromMessage(javax.jms.Message message) throws JMSException, MessageConversionException {
			if (message == null) {
				return null;
			}
			return new LazyResolutionMessage(message);
		}

		@Override
		protected Object extractPayload(Message message) throws JMSException {
			Object payload = extractMessage(message);
			if (message instanceof BytesMessage) {
				try {
					// 如果要将BytesMessage作为用户参数接收:
					// 重置它, 否则它对这样的处理代码似乎是空的...
					((BytesMessage) message).reset();
				}
				catch (JMSException ex) {
					// 继续, 因为BytesMessage通常不会再被使用.
					logger.debug("Failed to reset BytesMessage after payload extraction", ex);
				}
			}
			return payload;
		}

		@Override
		protected Message createMessageForPayload(Object payload, Session session, Object conversionHint)
				throws JMSException {

			MessageConverter converter = getMessageConverter();
			if (converter == null) {
				throw new IllegalStateException("No message converter, cannot handle '" + payload + "'");
			}
			if (converter instanceof SmartMessageConverter) {
				return ((SmartMessageConverter) converter).toMessage(payload, session, conversionHint);

			}
			return converter.toMessage(payload, session);
		}


		protected class LazyResolutionMessage implements org.springframework.messaging.Message<Object> {

			private final javax.jms.Message message;

			private Object payload;

			private MessageHeaders headers;

			public LazyResolutionMessage(javax.jms.Message message) {
				this.message = message;
			}

			@Override
			public Object getPayload() {
				if (this.payload == null) {
					try {
						this.payload = unwrapPayload();
					}
					catch (JMSException ex) {
						throw new MessageConversionException(
								"Failed to extract payload from [" + this.message + "]", ex);
					}
				}
				return this.payload;
			}

			/**
			 * 提取当前消息的有效负载.
			 * 由于推迟了有效负载的解析, 因此自定义转换器仍可能为其返回完整的消息.
			 * 在这种情况下, 返回其有效负载.
			 * 
			 * @return 消息的有效负载
			 */
			private Object unwrapPayload() throws JMSException {
				Object payload = extractPayload(this.message);
				if (payload instanceof org.springframework.messaging.Message) {
					return ((org.springframework.messaging.Message) payload).getPayload();
				}
				return payload;
			}

			@Override
			public MessageHeaders getHeaders() {
				if (this.headers == null) {
					this.headers = extractHeaders(this.message);
				}
				return this.headers;
			}
		}
	}


	/**
	 * 组合目标名称及其目标目标类型 (queue or topic)的内部类.
	 */
	private static class DestinationNameHolder {

		public final String name;

		public final boolean isTopic;

		public DestinationNameHolder(String name, boolean isTopic) {
			this.name = name;
			this.isTopic = isTopic;
		}
	}
}
