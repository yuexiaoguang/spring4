package org.springframework.jms.support.converter;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.springframework.util.ObjectUtils;

/**
 * 一个简单的消息转换器, 它能够处理 TextMessages, BytesMessages, MapMessages, ObjectMessages.
 * 用于{@link org.springframework.jms.core.JmsTemplate}的默认转换策略,
 * 用于{@code convertAndSend}和{@code receiveAndConvert}操作.
 *
 * <p>将String转换为{@link javax.jms.TextMessage}, a将字节数组转换为{@link javax.jms.BytesMessage},
 * 将Map转换为{@link javax.jms.MapMessage}, 将可序列化对象转换为{@link javax.jms.ObjectMessage} (反之亦然).
 */
public class SimpleMessageConverter implements MessageConverter {

	/**
	 * 此实现为String创建TextMessage, 为字节数组创建BytesMessage, 为Map创建MapMessage, 为可序列化对象创建ObjectMessage.
	 */
	@Override
	public Message toMessage(Object object, Session session) throws JMSException, MessageConversionException {
		if (object instanceof Message) {
			return (Message) object;
		}
		else if (object instanceof String) {
			return createMessageForString((String) object, session);
		}
		else if (object instanceof byte[]) {
			return createMessageForByteArray((byte[]) object, session);
		}
		else if (object instanceof Map) {
			return createMessageForMap((Map<? ,?>) object, session);
		}
		else if (object instanceof Serializable) {
			return createMessageForSerializable(((Serializable) object), session);
		}
		else {
			throw new MessageConversionException("Cannot convert object of type [" +
					ObjectUtils.nullSafeClassName(object) + "] to JMS message. Supported message " +
					"payloads are: String, byte array, Map<String,?>, Serializable object.");
		}
	}

	/**
	 * 此实现将TextMessage转换回String, 将ByteMessage转换回字节数组, 将MapMessage转换回Map, 将ObjectMessage转换回可序列化对象.
	 * 如果是未知的消息类型, 则返回普通的Message对象.
	 */
	@Override
	public Object fromMessage(Message message) throws JMSException, MessageConversionException {
		if (message instanceof TextMessage) {
			return extractStringFromMessage((TextMessage) message);
		}
		else if (message instanceof BytesMessage) {
			return extractByteArrayFromMessage((BytesMessage) message);
		}
		else if (message instanceof MapMessage) {
			return extractMapFromMessage((MapMessage) message);
		}
		else if (message instanceof ObjectMessage) {
			return extractSerializableFromMessage((ObjectMessage) message);
		}
		else {
			return message;
		}
	}


	/**
	 * 为给定的String创建JMS TextMessage.
	 * 
	 * @param text 要转换的字符串
	 * @param session 当前JMS会话
	 * 
	 * @return 结果消息
	 * @throws JMSException 如果由JMS方法抛出
	 */
	protected TextMessage createMessageForString(String text, Session session) throws JMSException {
		return session.createTextMessage(text);
	}

	/**
	 * 为给定的字节数组创建JMS BytesMessage.
	 * 
	 * @param bytes 要转换的字节数组
	 * @param session 当前JMS会话
	 * 
	 * @return 结果消息
	 * @throws JMSException 如果由JMS方法抛出
	 */
	protected BytesMessage createMessageForByteArray(byte[] bytes, Session session) throws JMSException {
		BytesMessage message = session.createBytesMessage();
		message.writeBytes(bytes);
		return message;
	}

	/**
	 * 为给定的Map创建JMS MapMessage.
	 * 
	 * @param map 要转换的Map
	 * @param session 当前JMS会话
	 * 
	 * @return 结果消息
	 * @throws JMSException 如果由JMS方法抛出
	 */
	protected MapMessage createMessageForMap(Map<?, ?> map, Session session) throws JMSException {
		MapMessage message = session.createMapMessage();
		for (Map.Entry<?, ?> entry : map.entrySet()) {
			Object key = entry.getKey();
			if (!(key instanceof String)) {
				throw new MessageConversionException("Cannot convert non-String key of type [" +
						ObjectUtils.nullSafeClassName(key) + "] to JMS MapMessage entry");
			}
			message.setObject((String) key, entry.getValue());
		}
		return message;
	}

	/**
	 * 为给定的可序列化对象创建JMS ObjectMessage.
	 * 
	 * @param object 要转换的可序列化对象
	 * @param session 当前JMS会话
	 * 
	 * @return 结果消息
	 * @throws JMSException 如果由JMS方法抛出
	 */
	protected ObjectMessage createMessageForSerializable(Serializable object, Session session) throws JMSException {
		return session.createObjectMessage(object);
	}


	/**
	 * 从给定的TextMessage中提取String.
	 * 
	 * @param message 要转换的消息
	 * 
	 * @return 结果字符串
	 * @throws JMSException 如果由JMS方法抛出
	 */
	protected String extractStringFromMessage(TextMessage message) throws JMSException {
		return message.getText();
	}

	/**
	 * 从给定的{@link BytesMessage}中提取字节数组.
	 * 
	 * @param message 要转换的消息
	 * 
	 * @return 结果字节数组
	 * @throws JMSException 如果由JMS方法抛出
	 */
	protected byte[] extractByteArrayFromMessage(BytesMessage message) throws JMSException {
		byte[] bytes = new byte[(int) message.getBodyLength()];
		message.readBytes(bytes);
		return bytes;
	}

	/**
	 * 从给定的{@link MapMessage}中提取Map.
	 * 
	 * @param message 要转换的消息
	 * 
	 * @return 结果Map
	 * @throws JMSException 如果由JMS方法抛出
	 */
	@SuppressWarnings("unchecked")
	protected Map<String, Object> extractMapFromMessage(MapMessage message) throws JMSException {
		Map<String, Object> map = new HashMap<String, Object>();
		Enumeration<String> en = message.getMapNames();
		while (en.hasMoreElements()) {
			String key = en.nextElement();
			map.put(key, message.getObject(key));
		}
		return map;
	}

	/**
	 * 从给定的{@link ObjectMessage}中提取可序列化对象.
	 * 
	 * @param message 要转换的消息
	 * 
	 * @return 结果可序列化对象
	 * @throws JMSException 如果由JMS方法抛出
	 */
	protected Serializable extractSerializableFromMessage(ObjectMessage message) throws JMSException {
		return message.getObject();
	}

}
