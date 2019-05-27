package org.springframework.messaging.converter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

/**
 * {@link SmartMessageConverter}实现的抽象基类, 包括对公共属性的支持和转换方法的部分实现,
 * 主要是检查转换器是否支持基于有效负载类和MIME类型的转换.
 */
public abstract class AbstractMessageConverter implements SmartMessageConverter {

	protected final Log logger = LogFactory.getLog(getClass());

	private final List<MimeType> supportedMimeTypes;

	private ContentTypeResolver contentTypeResolver = new DefaultContentTypeResolver();

	private boolean strictContentTypeMatch = false;

	private Class<?> serializedPayloadClass = byte[].class;


	/**
	 * @param supportedMimeType 支持的MIME类型
	 */
	protected AbstractMessageConverter(MimeType supportedMimeType) {
		Assert.notNull(supportedMimeType, "supportedMimeType is required");
		this.supportedMimeTypes = Collections.<MimeType>singletonList(supportedMimeType);
	}

	/**
	 * @param supportedMimeTypes 支持的MIME类型
	 */
	protected AbstractMessageConverter(Collection<MimeType> supportedMimeTypes) {
		Assert.notNull(supportedMimeTypes, "supportedMimeTypes must not be null");
		this.supportedMimeTypes = new ArrayList<MimeType>(supportedMimeTypes);
	}


	/**
	 * 返回支持的MIME类型.
	 */
	public List<MimeType> getSupportedMimeTypes() {
		return Collections.unmodifiableList(this.supportedMimeTypes);
	}

	/**
	 * 配置用于解析输入消息的内容类型的{@link ContentTypeResolver}.
	 * <p>请注意, 如果未配置解析器, 则
	 * {@link #setStrictContentTypeMatch(boolean) strictContentTypeMatch}应保留为{@code false} (默认值),
	 * 否则此转换器将忽略所有消息.
	 * <p>默认使用{@code DefaultContentTypeResolver}实例.
	 */
	public void setContentTypeResolver(ContentTypeResolver resolver) {
		this.contentTypeResolver = resolver;
	}

	/**
	 * 返回配置的{@link ContentTypeResolver}.
	 */
	public ContentTypeResolver getContentTypeResolver() {
		return this.contentTypeResolver;
	}

	/**
	 * 此转换器是否应转换配置的{@link org.springframework.messaging.converter.ContentTypeResolver}无法解析其内容类型的消息.
	 * <p>只有在配置{@link #setContentTypeResolver contentTypeResolver},
	 * 并且{@link #getSupportedMimeTypes() supportedMimeTypes}的列表不为空时, 才能将转换器配置为严格.
	 * <p>当此标志设置为{@code true}时, 如果未定义{@link #setContentTypeResolver contentTypeResolver}或者没有内容类型header,
	 * 则{@link #supportsMimeType(MessageHeaders)}将返回{@code false}.
	 */
	public void setStrictContentTypeMatch(boolean strictContentTypeMatch) {
		if (strictContentTypeMatch) {
			Assert.notEmpty(getSupportedMimeTypes(), "Strict match requires non-empty list of supported mime types");
			Assert.notNull(getContentTypeResolver(), "Strict match requires ContentTypeResolver");
		}
		this.strictContentTypeMatch = strictContentTypeMatch;
	}

	/**
	 * 内容类型解析是否必须生成与受支持的MIME类型之一匹配的值.
	 */
	public boolean isStrictContentTypeMatch() {
		return this.strictContentTypeMatch;
	}

	/**
	 * 将Object有效负载转换为{@link Message}时，配置要使用的首选序列化类 (byte[] 或 String).
	 * <p>默认值是 byte[].
	 * 
	 * @param payloadClass byte[] 或 String
	 */
	public void setSerializedPayloadClass(Class<?> payloadClass) {
		if (!(byte[].class == payloadClass || String.class == payloadClass)) {
			throw new IllegalArgumentException("Payload class must be byte[] or String: " + payloadClass);
		}
		this.serializedPayloadClass = payloadClass;
	}

	/**
	 * 返回已配置的首选序列化有效负载类.
	 */
	public Class<?> getSerializedPayloadClass() {
		return this.serializedPayloadClass;
	}


	/**
	 * 返回有效负载的默认内容类型.
	 * 在没有消息header或没有内容类型header的情况下调用{@link #toMessage(Object, MessageHeaders)}时调用.
	 * <p>默认情况下, 这将返回{@link #getSupportedMimeTypes() supportedMimeTypes}的第一个元素. 可以在子类中重写.
	 * 
	 * @param payload 要转换为消息的有效负载
	 * 
	 * @return 内容类型, 或{@code null}
	 */
	protected MimeType getDefaultContentType(Object payload) {
		List<MimeType> mimeTypes = getSupportedMimeTypes();
		return (!mimeTypes.isEmpty() ? mimeTypes.get(0) : null);
	}

	@Override
	public final Object fromMessage(Message<?> message, Class<?> targetClass) {
		return fromMessage(message, targetClass, null);
	}

	@Override
	public final Object fromMessage(Message<?> message, Class<?> targetClass, Object conversionHint) {
		if (!canConvertFrom(message, targetClass)) {
			return null;
		}
		return convertFromInternal(message, targetClass, conversionHint);
	}

	protected boolean canConvertFrom(Message<?> message, Class<?> targetClass) {
		return (supports(targetClass) && supportsMimeType(message.getHeaders()));
	}

	@Override
	public final Message<?> toMessage(Object payload, MessageHeaders headers) {
		return toMessage(payload, headers, null);
	}

	@Override
	public final Message<?> toMessage(Object payload, MessageHeaders headers, Object conversionHint) {
		if (!canConvertTo(payload, headers)) {
			return null;
		}

		payload = convertToInternal(payload, headers, conversionHint);
		if (payload == null) {
			return null;
		}

		MimeType mimeType = getDefaultContentType(payload);
		if (headers != null) {
			MessageHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(headers, MessageHeaderAccessor.class);
			if (accessor != null && accessor.isMutable()) {
				accessor.setHeaderIfAbsent(MessageHeaders.CONTENT_TYPE, mimeType);
				return MessageBuilder.createMessage(payload, accessor.getMessageHeaders());
			}
		}

		MessageBuilder<?> builder = MessageBuilder.withPayload(payload);
		if (headers != null) {
			builder.copyHeaders(headers);
		}
		builder.setHeaderIfAbsent(MessageHeaders.CONTENT_TYPE, mimeType);
		return builder.build();
	}

	protected boolean canConvertTo(Object payload, MessageHeaders headers) {
		Class<?> clazz = (payload != null ? payload.getClass() : null);
		return (supports(clazz) && supportsMimeType(headers));
	}

	protected boolean supportsMimeType(MessageHeaders headers) {
		if (getSupportedMimeTypes().isEmpty()) {
			return true;
		}
		MimeType mimeType = getMimeType(headers);
		if (mimeType == null) {
			return !isStrictContentTypeMatch();
		}
		for (MimeType current : getSupportedMimeTypes()) {
			if (current.getType().equals(mimeType.getType()) && current.getSubtype().equals(mimeType.getSubtype())) {
				return true;
			}
		}
		return false;
	}

	protected MimeType getMimeType(MessageHeaders headers) {
		return (this.contentTypeResolver != null ? this.contentTypeResolver.resolve(headers) : null);
	}


	/**
	 * 此转换器是否支持给定的类.
	 * 
	 * @param clazz 要测试是否支持的类
	 * 
	 * @return {@code true}如果支持; 否则{@code false}
	 */
	protected abstract boolean supports(Class<?> clazz);

	/**
	 * 将消息有效负载从序列化形式转换为Object.
	 * 
	 * @param message 输入消息
	 * @param targetClass 转换的目标类
	 * @param conversionHint 传递给{@link MessageConverter}的额外对象, e.g. 相关的{@code MethodParameter} (may be {@code null}}
	 * 
	 * @return 转换的结果, 或{@code null} 如果转换器无法执行转换
	 */
	@SuppressWarnings("deprecation")
	protected Object convertFromInternal(Message<?> message, Class<?> targetClass, Object conversionHint) {
		return convertFromInternal(message, targetClass);
	}

	/**
	 * 将有效负载对象转换为序列化形式.
	 * 
	 * @param payload 要转换的对象
	 * @param headers 消息的可选header (may be {@code null})
	 * @param conversionHint 传递给{@link MessageConverter}的额外对象, e.g. 相关的{@code MethodParameter} (may be {@code null}}
	 * 
	 * @return 消息的结果有效负载, 或{@code null} 如果转换器无法执行转换
	 */
	@SuppressWarnings("deprecation")
	protected Object convertToInternal(Object payload, MessageHeaders headers, Object conversionHint) {
		return convertToInternal(payload, headers);
	}

	/**
	 * 将消息有效负载从序列化形式转换为Object.
	 * 
	 * @deprecated as of Spring 4.2, in favor of {@link #convertFromInternal(Message, Class, Object)}
	 * (which is also protected instead of public)
	 */
	@Deprecated
	public Object convertFromInternal(Message<?> message, Class<?> targetClass) {
		return null;
	}

	/**
	 * 将有效负载对象转换为序列化形式.
	 * 
	 * @deprecated as of Spring 4.2, in favor of {@link #convertFromInternal(Message, Class, Object)}
	 * (which is also protected instead of public)
	 */
	@Deprecated
	public Object convertToInternal(Object payload, MessageHeaders headers) {
		return null;
	}
}
