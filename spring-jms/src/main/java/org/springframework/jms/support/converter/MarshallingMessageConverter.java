package org.springframework.jms.support.converter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.Unmarshaller;
import org.springframework.oxm.XmlMappingException;
import org.springframework.util.Assert;

/**
 * 使用{@link Marshaller}和{@link Unmarshaller}的Spring JMS {@link MessageConverter}.
 * 如果{@link #setTargetType targetType}设置为{@link MessageType#TEXT},
 * 则将对象编组为{@link BytesMessage}或{@link TextMessage}.
 * 从{@link TextMessage}或{@link BytesMessage}解组为对象.
 */
public class MarshallingMessageConverter implements MessageConverter, InitializingBean {

	private Marshaller marshaller;

	private Unmarshaller unmarshaller;

	private MessageType targetType = MessageType.BYTES;


	/**
	 * 编组后必须通过调用{@link #setMarshaller(Marshaller)}和{@link #setUnmarshaller(Unmarshaller)}来设置编组器.
	 */
	public MarshallingMessageConverter() {
	}

	/**
	 * <p>如果给定的{@link Marshaller}也实现了{@link Unmarshaller}接口, 它将用于编组和解组.
	 * 否则抛出异常.
	 * <p>请注意, Spring中的所有{@link Marshaller}实现也实现了{@link Unmarshaller}接口, 因此可以安全地使用此构造函数.
	 * 
	 * @param marshaller 用作编组器和解组器的对象
	 * 
	 * @throws IllegalArgumentException 当{@code marshaller}没有实现{@link Unmarshaller}接口时
	 */
	public MarshallingMessageConverter(Marshaller marshaller) {
		Assert.notNull(marshaller, "Marshaller must not be null");
		if (!(marshaller instanceof Unmarshaller)) {
			throw new IllegalArgumentException(
					"Marshaller [" + marshaller + "] does not implement the Unmarshaller " +
					"interface. Please set an Unmarshaller explicitly by using the " +
					"MarshallingMessageConverter(Marshaller, Unmarshaller) constructor.");
		}
		else {
			this.marshaller = marshaller;
			this.unmarshaller = (Unmarshaller) marshaller;
		}
	}

	/**
	 * @param marshaller 要使用的Marshaller
	 * @param unmarshaller 要使用的Unmarshaller
	 */
	public MarshallingMessageConverter(Marshaller marshaller, Unmarshaller unmarshaller) {
		Assert.notNull(marshaller, "Marshaller must not be null");
		Assert.notNull(unmarshaller, "Unmarshaller must not be null");
		this.marshaller = marshaller;
		this.unmarshaller = unmarshaller;
	}


	/**
	 * 设置此消息转换器使用的{@link Marshaller}.
	 */
	public void setMarshaller(Marshaller marshaller) {
		this.marshaller = marshaller;
	}

	/**
	 * 设置此消息转换器使用的{@link Unmarshaller}.
	 */
	public void setUnmarshaller(Unmarshaller unmarshaller) {
		this.unmarshaller = unmarshaller;
	}

	/**
	 * 指定{@link #toMessage(Object, Session)}是否应该编组为{@link BytesMessage}或 {@link TextMessage}.
	 * <p>默认值为{@link MessageType#BYTES}, i.e. 此转换器编组为{@link BytesMessage}.
	 * 请注意, 此转换器的默认版本仅支持{@link MessageType#BYTES}和{@link MessageType#TEXT}.
	 */
	public void setTargetType(MessageType targetType) {
		Assert.notNull(targetType, "MessageType must not be null");
		this.targetType = targetType;
	}

	@Override
	public void afterPropertiesSet() {
		Assert.notNull(this.marshaller, "Property 'marshaller' is required");
		Assert.notNull(this.unmarshaller, "Property 'unmarshaller' is required");
	}


	/**
	 * 此实现将给定对象编组为{@link javax.jms.TextMessage} 或 {@link javax.jms.BytesMessage}.
	 * 可以通过设置{@link #setTargetType "marshalTo"}属性来定义所需的消息类型.
	 */
	@Override
	public Message toMessage(Object object, Session session) throws JMSException, MessageConversionException {
		try {
			switch (this.targetType) {
				case TEXT:
					return marshalToTextMessage(object, session, this.marshaller);
				case BYTES:
					return marshalToBytesMessage(object, session, this.marshaller);
				default:
					return marshalToMessage(object, session, this.marshaller, this.targetType);
			}
		}
		catch (XmlMappingException ex) {
			throw new MessageConversionException("Could not marshal [" + object + "]", ex);
		}
		catch (IOException ex) {
			throw new MessageConversionException("Could not marshal [" + object + "]", ex);
		}
	}

	/**
	 * 此实现将给定的{@link Message}解组为对象.
	 */
	@Override
	public Object fromMessage(Message message) throws JMSException, MessageConversionException {
		try {
			if (message instanceof TextMessage) {
				TextMessage textMessage = (TextMessage) message;
				return unmarshalFromTextMessage(textMessage, this.unmarshaller);
			}
			else if (message instanceof BytesMessage) {
				BytesMessage bytesMessage = (BytesMessage) message;
				return unmarshalFromBytesMessage(bytesMessage, this.unmarshaller);
			}
			else {
				return unmarshalFromMessage(message, this.unmarshaller);
			}
		}
		catch (IOException ex) {
			throw new MessageConversionException("Could not access message content: " + message, ex);
		}
		catch (XmlMappingException ex) {
			throw new MessageConversionException("Could not unmarshal message: " + message, ex);
		}
	}


	/**
	 * 将给定对象编组为{@link TextMessage}.
	 * 
	 * @param object 要编组的对象
	 * @param session 当前的JMS会话
	 * @param marshaller 要使用的编组器
	 * 
	 * @return 结果消息
	 * @throws JMSException 如果由JMS方法抛出
	 * @throws IOException 在I/O错误的情况下
	 * @throws XmlMappingException 在OXM映射错误的情况下
	 */
	protected TextMessage marshalToTextMessage(Object object, Session session, Marshaller marshaller)
			throws JMSException, IOException, XmlMappingException {

		StringWriter writer = new StringWriter();
		Result result = new StreamResult(writer);
		marshaller.marshal(object, result);
		return session.createTextMessage(writer.toString());
	}

	/**
	 * 将给定对象编组为{@link BytesMessage}.
	 * 
	 * @param object 要编组的对象
	 * @param session 当前的JMS会话
	 * @param marshaller 要使用的编组器
	 * 
	 * @return 结果消息
	 * @throws JMSException 如果由JMS方法抛出
	 * @throws IOException 在I/O错误的情况下
	 * @throws XmlMappingException 在OXM映射错误的情况下
	 */
	protected BytesMessage marshalToBytesMessage(Object object, Session session, Marshaller marshaller)
			throws JMSException, IOException, XmlMappingException {

		ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
		StreamResult streamResult = new StreamResult(bos);
		marshaller.marshal(object, streamResult);
		BytesMessage message = session.createBytesMessage();
		message.writeBytes(bos.toByteArray());
		return message;
	}

	/**
	 * 允许自定义消息编组的模板方法.
	 * 当{@link #setTargetType}不是{@link MessageType#TEXT} 或 {@link MessageType#BYTES}时调用.
	 * <p>默认实现抛出{@link IllegalArgumentException}.
	 * 
	 * @param object 要编组的对象
	 * @param session JMS会话
	 * @param marshaller 要使用的编组器
	 * @param targetType 目标消息类型 (TEXT或BYTES除外)
	 * 
	 * @return 结果消息
	 * @throws JMSException 如果由JMS方法抛出
	 * @throws IOException 在I/O错误的情况下
	 * @throws XmlMappingException 在OXM映射错误的情况下
	 */
	protected Message marshalToMessage(Object object, Session session, Marshaller marshaller, MessageType targetType)
			throws JMSException, IOException, XmlMappingException {

		throw new IllegalArgumentException("Unsupported message type [" + targetType +
				"]. MarshallingMessageConverter by default only supports TextMessages and BytesMessages.");
	}


	/**
	 * 将给定的{@link TextMessage}解组为对象.
	 * 
	 * @param message 消息
	 * @param unmarshaller 要使用的解组器
	 * 
	 * @return 解组后的对象
	 * @throws JMSException 如果由JMS方法抛出
	 * @throws IOException 在I/O错误的情况下
	 * @throws XmlMappingException 在OXM映射错误的情况下
	 */
	protected Object unmarshalFromTextMessage(TextMessage message, Unmarshaller unmarshaller)
			throws JMSException, IOException, XmlMappingException {

		Source source = new StreamSource(new StringReader(message.getText()));
		return unmarshaller.unmarshal(source);
	}

	/**
	 * 将给定的{@link BytesMessage}解组为对象.
	 * 
	 * @param message 消息
	 * @param unmarshaller 要使用的解组器
	 * 
	 * @return 解组后的对象
	 * @throws JMSException 如果由JMS方法抛出
	 * @throws IOException 在I/O错误的情况下
	 * @throws XmlMappingException 在OXM映射错误的情况下
	 */
	protected Object unmarshalFromBytesMessage(BytesMessage message, Unmarshaller unmarshaller)
			throws JMSException, IOException, XmlMappingException {

		byte[] bytes = new byte[(int) message.getBodyLength()];
		message.readBytes(bytes);
		ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
		StreamSource source = new StreamSource(bis);
		return unmarshaller.unmarshal(source);
	}

	/**
	 * 允许自定义消息解组的模板方法.
	 * 使用不是{@link TextMessage}或{@link BytesMessage}的消息调用{@link #fromMessage(Message)}时调用.
	 * <p>默认实现抛出{@link IllegalArgumentException}.
	 * 
	 * @param message 消息
	 * @param unmarshaller 要使用的解组器
	 * 
	 * @return 解组后的对象
	 * @throws JMSException 如果由JMS方法抛出
	 * @throws IOException 在I/O错误的情况下
	 * @throws XmlMappingException 在OXM映射错误的情况下
	 */
	protected Object unmarshalFromMessage(Message message, Unmarshaller unmarshaller)
			throws JMSException, IOException, XmlMappingException {

		throw new IllegalArgumentException("Unsupported message type [" + message.getClass() +
				"]. MarshallingMessageConverter by default only supports TextMessages and BytesMessages.");
	}
}
