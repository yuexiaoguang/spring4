package org.springframework.jms.support;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;

import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.AbstractHeaderMapper;
import org.springframework.util.StringUtils;

/**
 * {@link JmsHeaderMapper}的简单实现.
 *
 * <p>此实现将 JMS API header (e.g. JMSReplyTo)复制到{@link org.springframework.messaging.Message Messages}.
 * 任何用户定义的属性也将从JMS Message复制到 Message,
 * 并且Message上的任何其他header (JMS API header之外)也将同样复制到JMS Message.
 * 那些其他header将被复制到JMS Message的常规属性, 而JMS API header将被传递给适当的setter方法 (e.g. setJMSReplyTo).
 *
 * <p>JMS API header的常量在{@link JmsHeaders}中定义.
 * 请注意，大多数JMS header是只读的:
 * JMSDestination, JMSDeliveryMode, JMSExpiration, JMSMessageID, JMSPriority,
 * JMSRedelivered 和 JMSTimestamp标志仅从JMS Message中复制.
 * 这些值将<em>不</em>从Message传递到出站JMS Message.
 */
public class SimpleJmsHeaderMapper extends AbstractHeaderMapper<Message> implements JmsHeaderMapper {

	private static final Set<Class<?>> SUPPORTED_PROPERTY_TYPES = new HashSet<Class<?>>(Arrays.asList(new Class<?>[] {
			Boolean.class, Byte.class, Double.class, Float.class, Integer.class, Long.class, Short.class, String.class}));


	@Override
	public void fromHeaders(MessageHeaders headers, javax.jms.Message jmsMessage) {
		try {
			Object jmsCorrelationId = headers.get(JmsHeaders.CORRELATION_ID);
			if (jmsCorrelationId instanceof Number) {
				jmsCorrelationId = jmsCorrelationId.toString();
			}
			if (jmsCorrelationId instanceof String) {
				try {
					jmsMessage.setJMSCorrelationID((String) jmsCorrelationId);
				}
				catch (Exception ex) {
					logger.info("Failed to set JMSCorrelationID - skipping", ex);
				}
			}
			Destination jmsReplyTo = getHeaderIfAvailable(headers, JmsHeaders.REPLY_TO, Destination.class);
			if (jmsReplyTo != null) {
				try {
					jmsMessage.setJMSReplyTo(jmsReplyTo);
				}
				catch (Exception ex) {
					logger.info("Failed to set JMSReplyTo - skipping", ex);
				}
			}
			String jmsType = getHeaderIfAvailable(headers, JmsHeaders.TYPE, String.class);
			if (jmsType != null) {
				try {
					jmsMessage.setJMSType(jmsType);
				}
				catch (Exception ex) {
					logger.info("Failed to set JMSType - skipping", ex);
				}
			}
			Set<String> headerNames = headers.keySet();
			for (String headerName : headerNames) {
				if (StringUtils.hasText(headerName) && !headerName.startsWith(JmsHeaders.PREFIX)) {
					Object value = headers.get(headerName);
					if (value != null && SUPPORTED_PROPERTY_TYPES.contains(value.getClass())) {
						try {
							String propertyName = this.fromHeaderName(headerName);
							jmsMessage.setObjectProperty(propertyName, value);
						}
						catch (Exception ex) {
							if (headerName.startsWith("JMSX")) {
								if (logger.isTraceEnabled()) {
									logger.trace("Skipping reserved header '" + headerName +
											"' since it cannot be set by client");
								}
							}
							else if (logger.isWarnEnabled()) {
								logger.warn("Failed to map message header '" + headerName + "' to JMS property", ex);
							}
						}
					}
				}
			}
		}
		catch (Exception ex) {
			if (logger.isWarnEnabled()) {
				logger.warn("Error occurred while mapping from MessageHeaders to JMS properties", ex);
			}
		}
	}

	@Override
	public MessageHeaders toHeaders(javax.jms.Message jmsMessage) {
		Map<String, Object> headers = new HashMap<String, Object>();
		try {
			try {
				String correlationId = jmsMessage.getJMSCorrelationID();
				if (correlationId != null) {
					headers.put(JmsHeaders.CORRELATION_ID, correlationId);
				}
			}
			catch (Exception ex) {
				logger.info("Failed to read JMSCorrelationID property - skipping", ex);
			}
			try {
				Destination destination = jmsMessage.getJMSDestination();
				if (destination != null) {
					headers.put(JmsHeaders.DESTINATION, destination);
				}
			}
			catch (Exception ex) {
				logger.info("Failed to read JMSDestination property - skipping", ex);
			}
			try {
				int deliveryMode = jmsMessage.getJMSDeliveryMode();
				headers.put(JmsHeaders.DELIVERY_MODE, deliveryMode);
			}
			catch (Exception ex) {
				logger.info("Failed to read JMSDeliveryMode property - skipping", ex);
			}
			try {
				long expiration = jmsMessage.getJMSExpiration();
				headers.put(JmsHeaders.EXPIRATION, expiration);
			}
			catch (Exception ex) {
				logger.info("Failed to read JMSExpiration property - skipping", ex);
			}
			try {
				String messageId = jmsMessage.getJMSMessageID();
				if (messageId != null) {
					headers.put(JmsHeaders.MESSAGE_ID, messageId);
				}
			}
			catch (Exception ex) {
				logger.info("Failed to read JMSMessageID property - skipping", ex);
			}
			try {
				headers.put(JmsHeaders.PRIORITY, jmsMessage.getJMSPriority());
			}
			catch (Exception ex) {
				logger.info("Failed to read JMSPriority property - skipping", ex);
			}
			try {
				Destination replyTo = jmsMessage.getJMSReplyTo();
				if (replyTo != null) {
					headers.put(JmsHeaders.REPLY_TO, replyTo);
				}
			}
			catch (Exception ex) {
				logger.info("Failed to read JMSReplyTo property - skipping", ex);
			}
			try {
				headers.put(JmsHeaders.REDELIVERED, jmsMessage.getJMSRedelivered());
			}
			catch (Exception ex) {
				logger.info("Failed to read JMSRedelivered property - skipping", ex);
			}
			try {
				String type = jmsMessage.getJMSType();
				if (type != null) {
					headers.put(JmsHeaders.TYPE, type);
				}
			}
			catch (Exception ex) {
				logger.info("Failed to read JMSType property - skipping", ex);
			}
			try {
				headers.put(JmsHeaders.TIMESTAMP, jmsMessage.getJMSTimestamp());
			}
			catch (Exception ex) {
				logger.info("Failed to read JMSTimestamp property - skipping", ex);
			}

			Enumeration<?> jmsPropertyNames = jmsMessage.getPropertyNames();
			if (jmsPropertyNames != null) {
				while (jmsPropertyNames.hasMoreElements()) {
					String propertyName = jmsPropertyNames.nextElement().toString();
					try {
						String headerName = this.toHeaderName(propertyName);
						headers.put(headerName, jmsMessage.getObjectProperty(propertyName));
					}
					catch (Exception ex) {
						if (logger.isWarnEnabled()) {
							logger.warn("Error occurred while mapping JMS property '" + propertyName +
									"' to Message header", ex);
						}
					}
				}
			}
		}
		catch (JMSException ex) {
			if (logger.isWarnEnabled()) {
				logger.warn("Error occurred while mapping from JMS properties to MessageHeaders", ex);
			}
		}
		return new MessageHeaders(headers);
	}

	/**
	 * 添加出站前缀.
	 * <p>将{@link MessageHeaders#CONTENT_TYPE}转换为{@code content_type}以符合JMS.
	 */
	@Override
	protected String fromHeaderName(String headerName) {
		if (MessageHeaders.CONTENT_TYPE.equals(headerName)) {
			return CONTENT_TYPE_PROPERTY;
		}
		return super.fromHeaderName(headerName);
	}

	/**
	 * 添加入站前缀.
	 * <p>将符合JMS的{@code content_type}转换为{@link MessageHeaders#CONTENT_TYPE}.
	 */
	@Override
	protected String toHeaderName(String propertyName) {
		if (CONTENT_TYPE_PROPERTY.equals(propertyName)) {
			return MessageHeaders.CONTENT_TYPE;
		}
		return super.toHeaderName(propertyName);
	}
}
