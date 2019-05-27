package org.springframework.jms.support.converter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.core.MethodParameter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * 使用Jackson 2.x将消息转换为JSON和从JSON转换消息的消息转换器.
 * 如果{@link #setTargetType targetType}设置为{@link MessageType#TEXT}, 则将对象映射到{@link BytesMessage}或{@link TextMessage}.
 * 从{@link TextMessage}或{@link BytesMessage}转换为对象.
 *
 * <p>它通过以下方式定制Jackson的默认属性:
 * <ul>
 * <li>禁用{@link MapperFeature#DEFAULT_VIEW_INCLUSION}</li>
 * <li>禁用{@link DeserializationFeature#FAIL_ON_UNKNOWN_PROPERTIES}</li>
 * </ul>
 *
 * <p>从Spring 4.3开始, 与Jackson 2.6及更高版本兼容.
 */
public class MappingJackson2MessageConverter implements SmartMessageConverter, BeanClassLoaderAware {

	/**
	 * 用于写入文本消息的默认编码: UTF-8.
	 */
	public static final String DEFAULT_ENCODING = "UTF-8";


	private ObjectMapper objectMapper;

	private MessageType targetType = MessageType.BYTES;

	private String encoding = DEFAULT_ENCODING;

	private String encodingPropertyName;

	private String typeIdPropertyName;

	private Map<String, Class<?>> idClassMappings = new HashMap<String, Class<?>>();

	private Map<Class<?>, String> classIdMappings = new HashMap<Class<?>, String>();

	private ClassLoader beanClassLoader;


	public MappingJackson2MessageConverter() {
		this.objectMapper = new ObjectMapper();
		this.objectMapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false);
		this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	/**
	 * 指定要使用的{@link ObjectMapper}而不是使用默认值.
	 */
	public void setObjectMapper(ObjectMapper objectMapper) {
		Assert.notNull(objectMapper, "ObjectMapper must not be null");
		this.objectMapper = objectMapper;
	}

	/**
	 * 指定{@link #toMessage(Object, Session)}是否应该编组为 {@link BytesMessage}或{@link TextMessage}.
	 * <p>默认值为{@link MessageType#BYTES}, i.e. 此转换器编组为{@link BytesMessage}.
	 * 请注意, 此转换器的默认版本仅支持{@link MessageType#BYTES} 和 {@link MessageType#TEXT}.
	 */
	public void setTargetType(MessageType targetType) {
		Assert.notNull(targetType, "MessageType must not be null");
		this.targetType = targetType;
	}

	/**
	 * 指定转换基于文本的消息正文内容时要使用的编码. 默认编码为 "UTF-8".
	 * <p>从基于文本的消息中读取时, 可能已经通过特殊的JMS属性建议了编码, 该属性将优先于此MessageConverter实例上的编码集.
	 */
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	/**
	 * 指定JMS消息属性的名称, 该属性表示字节和字符串转换时的编码, 在转换过程中使用BytesMessage.
	 * <p>默认无. 设置此属性是可选的; 如果没有设置, UTF-8将用于解码任何传入的字节消息.
	 */
	public void setEncodingPropertyName(String encodingPropertyName) {
		this.encodingPropertyName = encodingPropertyName;
	}

	/**
	 * 指定包含所包含对象的类型ID的JMS消息属性的名称: 映射的id值或原始Java类名.
	 * <p>默认无. <b>NOTE: 需要设置此属性以允许从传入消息转换为Java对象.</b>
	 */
	public void setTypeIdPropertyName(String typeIdPropertyName) {
		this.typeIdPropertyName = typeIdPropertyName;
	}

	/**
	 * 如果需要, 指定从类型ID到Java类的映射.
	 * 这允许在类型id消息属性中使用合成ID, 而不是传输Java类名.
	 * <p>默认是没有自定义映射, i.e. 传输原始Java类名.
	 * 
	 * @param typeIdMappings 类型id值作为键, Java类作为值的Map
	 */
	public void setTypeIdMappings(Map<String, Class<?>> typeIdMappings) {
		this.idClassMappings = new HashMap<String, Class<?>>();
		for (Map.Entry<String, Class<?>> entry : typeIdMappings.entrySet()) {
			String id = entry.getKey();
			Class<?> clazz = entry.getValue();
			this.idClassMappings.put(id, clazz);
			this.classIdMappings.put(clazz, id);
		}
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}


	@Override
	public Message toMessage(Object object, Session session) throws JMSException, MessageConversionException {
		Message message;
		try {
			switch (this.targetType) {
				case TEXT:
					message = mapToTextMessage(object, session, this.objectMapper);
					break;
				case BYTES:
					message = mapToBytesMessage(object, session, this.objectMapper);
					break;
				default:
					message = mapToMessage(object, session, this.objectMapper, this.targetType);
			}
		}
		catch (IOException ex) {
			throw new MessageConversionException("Could not map JSON object [" + object + "]", ex);
		}
		setTypeIdOnMessage(object, message);
		return message;
	}

	@Override
	public Message toMessage(Object object, Session session, Object conversionHint)
			throws JMSException, MessageConversionException {

		return toMessage(object, session, getSerializationView(conversionHint));
	}

	/**
	 * 使用指定的json视图和提供的会话将Java对象转换为JMS消息以创建消息对象.
	 * 
	 * @param object 要转换的对象
	 * @param session 用于创建JMS消息的会话
	 * @param jsonView 用于过滤内容的视图
	 * 
	 * @return the JMS Message
	 * @throws javax.jms.JMSException 如果由JMS API方法抛出
	 * @throws MessageConversionException 如果转换失败
	 */
	public Message toMessage(Object object, Session session, Class<?> jsonView)
			throws JMSException, MessageConversionException {

		if (jsonView != null) {
			return toMessage(object, session, this.objectMapper.writerWithView(jsonView));
		}
		else {
			return toMessage(object, session, this.objectMapper.writer());
		}
	}

	@Override
	public Object fromMessage(Message message) throws JMSException, MessageConversionException {
		try {
			JavaType targetJavaType = getJavaTypeForMessage(message);
			return convertToObject(message, targetJavaType);
		}
		catch (IOException ex) {
			throw new MessageConversionException("Failed to convert JSON message content", ex);
		}
	}

	protected Message toMessage(Object object, Session session, ObjectWriter objectWriter)
			throws JMSException, MessageConversionException {

		Message message;
		try {
			switch (this.targetType) {
				case TEXT:
					message = mapToTextMessage(object, session, objectWriter);
					break;
				case BYTES:
					message = mapToBytesMessage(object, session, objectWriter);
					break;
				default:
					message = mapToMessage(object, session, objectWriter, this.targetType);
			}
		}
		catch (IOException ex) {
			throw new MessageConversionException("Could not map JSON object [" + object + "]", ex);
		}
		setTypeIdOnMessage(object, message);
		return message;
	}


	/**
	 * 将给定对象映射到{@link TextMessage}.
	 * 
	 * @param object 要映射的对象
	 * @param session 当前的JMS会话
	 * @param objectMapper 要使用的映射器
	 * 
	 * @return 结果消息
	 * @throws JMSException 如果由JMS方法抛出
	 * @throws IOException 发生I/O错误
	 * 
	 * @deprecated as of 4.3, use {@link #mapToTextMessage(Object, Session, ObjectWriter)}
	 */
	@Deprecated
	protected TextMessage mapToTextMessage(Object object, Session session, ObjectMapper objectMapper)
			throws JMSException, IOException {

		return mapToTextMessage(object, session, objectMapper.writer());
	}

	/**
	 * 将给定对象映射到{@link TextMessage}.
	 * 
	 * @param object 要映射的对象
	 * @param session 当前的JMS会话
	 * @param objectWriter 要使用的写入器
	 * 
	 * @return 结果消息
	 * @throws JMSException 如果由JMS方法抛出
	 * @throws IOException 发生I/O错误
	 */
	protected TextMessage mapToTextMessage(Object object, Session session, ObjectWriter objectWriter)
			throws JMSException, IOException {

		StringWriter writer = new StringWriter();
		objectWriter.writeValue(writer, object);
		return session.createTextMessage(writer.toString());
	}

	/**
	 * 将给定对象映射到{@link BytesMessage}.
	 * 
	 * @param object 要映射的对象
	 * @param session 当前的JMS会话
	 * @param objectMapper 要使用的映射器
	 * 
	 * @return 结果消息
	 * @throws JMSException 如果由JMS方法抛出
	 * @throws IOException 发生I/O错误
	 * 
	 * @deprecated as of 4.3, use {@link #mapToBytesMessage(Object, Session, ObjectWriter)}
	 */
	@Deprecated
	protected BytesMessage mapToBytesMessage(Object object, Session session, ObjectMapper objectMapper)
			throws JMSException, IOException {

		return mapToBytesMessage(object, session, objectMapper.writer());
	}


	/**
	 * 将给定对象映射到{@link BytesMessage}.
	 * 
	 * @param object 要映射的对象
	 * @param session 当前的JMS会话
	 * @param objectWriter 要使用的写入器
	 * 
	 * @return 结果消息
	 * @throws JMSException 如果由JMS方法抛出
	 * @throws IOException 发生I/O错误
	 */
	protected BytesMessage mapToBytesMessage(Object object, Session session, ObjectWriter objectWriter)
			throws JMSException, IOException {

		ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
		OutputStreamWriter writer = new OutputStreamWriter(bos, this.encoding);
		objectWriter.writeValue(writer, object);

		BytesMessage message = session.createBytesMessage();
		message.writeBytes(bos.toByteArray());
		if (this.encodingPropertyName != null) {
			message.setStringProperty(this.encodingPropertyName, this.encoding);
		}
		return message;
	}

	/**
	 * 允许自定义消息映射的模板方法.
	 * {@link #setTargetType}不是{@link MessageType#TEXT}或{@link MessageType#BYTES}时调用.
	 * <p>默认实现抛出{@link IllegalArgumentException}.
	 * 
	 * @param object 要编组的对象
	 * @param session the JMS Session
	 * @param objectMapper 要使用的映射器
	 * @param targetType 目标消息类型 (TEXT或BYTES除外)
	 * 
	 * @return 结果消息
	 * @throws JMSException 如果由JMS方法抛出
	 * @throws IOException 发生I/O错误
	 * @deprecated as of 4.3, use {@link #mapToMessage(Object, Session, ObjectWriter, MessageType)}
	 */
	@Deprecated
	protected Message mapToMessage(Object object, Session session, ObjectMapper objectMapper, MessageType targetType)
			throws JMSException, IOException {

		return mapToMessage(object, session, objectMapper.writer(), targetType);
	}

	/**
	 * 允许自定义消息映射的模板方法.
	 * {@link #setTargetType}不是{@link MessageType#TEXT}或{@link MessageType#BYTES}时调用.
	 * <p>默认实现抛出{@link IllegalArgumentException}.
	 * 
	 * @param object 要编组的对象
	 * @param session the JMS Session
	 * @param objectWriter 要使用的写入器
	 * @param targetType 目标消息类型 (TEXT或BYTES除外)
	 * 
	 * @return 结果消息
	 * @throws JMSException 如果由JMS方法抛出
	 * @throws IOException 发生I/O错误
	 */
	protected Message mapToMessage(Object object, Session session, ObjectWriter objectWriter, MessageType targetType)
			throws JMSException, IOException {

		throw new IllegalArgumentException("Unsupported message type [" + targetType +
				"]. MappingJackson2MessageConverter by default only supports TextMessages and BytesMessages.");
	}

	/**
	 * 在给定的JMS消息上为给定的有效负载对象设置类型ID.
	 * <p>默认实现参考配置的类型id映射, 并将结果值 (映射的id或原始Java类名称) 设置为已配置的类型 id消息属性.
	 * 
	 * @param object 要设置类型ID的有效负载对象
	 * @param message 要设置类型ID的JMS消息
	 * 
	 * @throws JMSException 如果由JMS方法抛出
	 */
	protected void setTypeIdOnMessage(Object object, Message message) throws JMSException {
		if (this.typeIdPropertyName != null) {
			String typeId = this.classIdMappings.get(object.getClass());
			if (typeId == null) {
				typeId = object.getClass().getName();
			}
			message.setStringProperty(this.typeIdPropertyName, typeId);
		}
	}

	/**
	 * 为各个消息类型分配转换器.
	 */
	private Object convertToObject(Message message, JavaType targetJavaType) throws JMSException, IOException {
		if (message instanceof TextMessage) {
			return convertFromTextMessage((TextMessage) message, targetJavaType);
		}
		else if (message instanceof BytesMessage) {
			return convertFromBytesMessage((BytesMessage) message, targetJavaType);
		}
		else {
			return convertFromMessage(message, targetJavaType);
		}
	}

	/**
	 * 将TextMessage转换为具有指定类型的Java对象.
	 * 
	 * @param message 输入消息
	 * @param targetJavaType 目标类型
	 * 
	 * @return 转换为对象的消息
	 * @throws JMSException 如果被JMS抛出
	 * @throws IOException 发生I/O错误
	 */
	protected Object convertFromTextMessage(TextMessage message, JavaType targetJavaType)
			throws JMSException, IOException {

		String body = message.getText();
		return this.objectMapper.readValue(body, targetJavaType);
	}

	/**
	 * 将BytesMessage转换为具有指定类型的Java对象.
	 * 
	 * @param message 输入消息
	 * @param targetJavaType 目标类型
	 * 
	 * @return 转换为对象的消息
	 * @throws JMSException 如果被JMS抛出
	 * @throws IOException 发生I/O错误
	 */
	protected Object convertFromBytesMessage(BytesMessage message, JavaType targetJavaType)
			throws JMSException, IOException {

		String encoding = this.encoding;
		if (this.encodingPropertyName != null && message.propertyExists(this.encodingPropertyName)) {
			encoding = message.getStringProperty(this.encodingPropertyName);
		}
		byte[] bytes = new byte[(int) message.getBodyLength()];
		message.readBytes(bytes);
		try {
			String body = new String(bytes, encoding);
			return this.objectMapper.readValue(body, targetJavaType);
		}
		catch (UnsupportedEncodingException ex) {
			throw new MessageConversionException("Cannot convert bytes to String", ex);
		}
	}

	/**
	 * 允许自定义消息映射的模板方法.
	 * {@link #setTargetType}不是{@link MessageType#TEXT}或{@link MessageType#BYTES}时调用.
	 * <p>默认实现抛出{@link IllegalArgumentException}.
	 * 
	 * @param message 输入消息
	 * @param targetJavaType 目标类型
	 * 
	 * @return 转换为对象的消息
	 * @throws JMSException 如果被JMS抛出
	 * @throws IOException 发生I/O错误
	 */
	protected Object convertFromMessage(Message message, JavaType targetJavaType)
			throws JMSException, IOException {

		throw new IllegalArgumentException("Unsupported message type [" + message.getClass() +
				"]. MappingJacksonMessageConverter by default only supports TextMessages and BytesMessages.");
	}

	/**
	 * 确定给定JMS消息的Jackson JavaType, 通常解析类型ID消息属性.
	 * <p>默认实现解析配置的类型id属性名称, 并查询配置的类型id映射.
	 * 可以用不同的策略覆盖, e.g. 根据消息来源做一些启发式方法.
	 * 
	 * @param message 要设置类型ID的JMS消息
	 * 
	 * @throws JMSException 如果由JMS方法抛出
	 */
	protected JavaType getJavaTypeForMessage(Message message) throws JMSException {
		String typeId = message.getStringProperty(this.typeIdPropertyName);
		if (typeId == null) {
			throw new MessageConversionException(
					"Could not find type id property [" + this.typeIdPropertyName + "] on message [" +
					message.getJMSMessageID() + "] from destination [" + message.getJMSDestination() + "]");
		}
		Class<?> mappedClass = this.idClassMappings.get(typeId);
		if (mappedClass != null) {
			return this.objectMapper.getTypeFactory().constructType(mappedClass);
		}
		try {
			Class<?> typeClass = ClassUtils.forName(typeId, this.beanClassLoader);
			return this.objectMapper.getTypeFactory().constructType(typeClass);
		}
		catch (Throwable ex) {
			throw new MessageConversionException("Failed to resolve type id [" + typeId + "]", ex);
		}
	}

	/**
	 * 根据给定的转换提示确定Jackson序列化视图.
	 * 
	 * @param conversionHint 为当前转换尝试传递到转换器的转换提示对象
	 * 
	 * @return 序列化视图类, 或{@code null}
	 */
	protected Class<?> getSerializationView(Object conversionHint) {
		if (conversionHint instanceof MethodParameter) {
			MethodParameter methodParam = (MethodParameter) conversionHint;
			JsonView annotation = methodParam.getParameterAnnotation(JsonView.class);
			if (annotation == null) {
				annotation = methodParam.getMethodAnnotation(JsonView.class);
				if (annotation == null) {
					return null;
				}
			}
			return extractViewClass(annotation, conversionHint);
		}
		else if (conversionHint instanceof JsonView) {
			return extractViewClass((JsonView) conversionHint, conversionHint);
		}
		else if (conversionHint instanceof Class) {
			return (Class<?>) conversionHint;
		}
		else {
			return null;
		}
	}

	private Class<?> extractViewClass(JsonView annotation, Object conversionHint) {
		Class<?>[] classes = annotation.value();
		if (classes.length != 1) {
			throw new IllegalArgumentException(
					"@JsonView only supported for handler methods with exactly 1 class argument: " + conversionHint);
		}
		return classes[0];
	}
}
