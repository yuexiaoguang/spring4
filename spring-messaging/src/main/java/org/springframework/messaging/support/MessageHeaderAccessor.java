package org.springframework.messaging.support;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;
import org.springframework.util.IdGenerator;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.PatternMatchUtils;
import org.springframework.util.StringUtils;

/**
 * 提供强类型getter和setter的类的基础, 以及围绕特定类别的header的行为 (e.g. STOMP headers).
 * 支持创建新header, 修改现有header (仍然可变), 或复制和修改现有 header.
 *
 * <p>方法{@link #getMessageHeaders()}提供对底层的, 完全准备好的{@link MessageHeaders}的访问,
 * 然后可以按原样使用 (i.e. 不复制)来创建单个消息, 如下所示:
 *
 * <pre class="code">
 * MessageHeaderAccessor accessor = new MessageHeaderAccessor();
 * accessor.setHeader("foo", "bar");
 * Message message = MessageBuilder.createMessage("payload", accessor.getMessageHeaders());
 * </pre>
 *
 * <p>在上述之后, 默认情况下{@code MessageHeaderAccessor}变为不可变的.
 * 但是, 可以使其在同一线程中进行进一步初始化是可变的, 例如:
 *
 * <pre class="code">
 * MessageHeaderAccessor accessor = new MessageHeaderAccessor();
 * accessor.setHeader("foo", "bar");
 * accessor.setLeaveMutable(true);
 * Message message = MessageBuilder.createMessage("payload", accessor.getMessageHeaders());
 *
 * // later on in the same thread...
 *
 * MessageHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message);
 * accessor.setHeader("bar", "baz");
 * accessor.setImmutable();
 * </pre>
 *
 * <p>方法 {@link #toMap()}返回底层header的副本.
 * 它可用于从同一{@code MessageHeaderAccessor}实例准备多条消息:
 * <pre class="code">
 * MessageHeaderAccessor accessor = new MessageHeaderAccessor();
 * MessageBuilder builder = MessageBuilder.withPayload("payload").setHeaders(accessor);
 *
 * accessor.setHeader("foo", "bar1");
 * Message message1 = builder.build();
 *
 * accessor.setHeader("foo", "bar2");
 * Message message2 = builder.build();
 *
 * accessor.setHeader("foo", "bar3");
 * Message  message3 = builder.build();
 * </pre>
 *
 * <p>但请注意, 使用上述样式时, header访问器是共享的, 以后无法重新获取.
 * 或者, 也可以为每条消息创建一个{@code MessageHeaderAccessor}:
 *
 * <pre class="code">
 * MessageHeaderAccessor accessor1 = new MessageHeaderAccessor();
 * accessor.set("foo", "bar1");
 * Message message1 = MessageBuilder.createMessage("payload", accessor1.getMessageHeaders());
 *
 * MessageHeaderAccessor accessor2 = new MessageHeaderAccessor();
 * accessor.set("foo", "bar2");
 * Message message2 = MessageBuilder.createMessage("payload", accessor2.getMessageHeaders());
 *
 * MessageHeaderAccessor accessor3 = new MessageHeaderAccessor();
 * accessor.set("foo", "bar3");
 * Message message3 = MessageBuilder.createMessage("payload", accessor3.getMessageHeaders());
 * </pre>
 *
 * <p>请注意, 上述示例旨在演示使用header访问器的一般概念.
 * 然而, 最可能的用法是通过子类.
 */
public class MessageHeaderAccessor {

	public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

	private static final MimeType[] READABLE_MIME_TYPES = new MimeType[] {
			MimeTypeUtils.APPLICATION_JSON, MimeTypeUtils.APPLICATION_XML,
			new MimeType("text", "*"), new MimeType("application", "*+json"), new MimeType("application", "*+xml")
	};


	private final MutableMessageHeaders headers;

	private boolean leaveMutable = false;

	private boolean modified = false;

	private boolean enableTimestamp = false;

	private IdGenerator idGenerator;


	public MessageHeaderAccessor() {
		this(null);
	}

	/**
	 * 接受要复制的现有消息的header.
	 * 
	 * @param message 要从中复制header的消息, 或{@code null}
	 */
	public MessageHeaderAccessor(Message<?> message) {
		this.headers = new MutableMessageHeaders(message != null ? message.getHeaders() : null);
	}


	/**
	 * 为给定的消息构建一个'嵌套'访问器.
	 * 
	 * @param message 用于构建新访问器的消息
	 * 
	 * @return 嵌套的访问器 (通常是特定的子类)
	 */
	protected MessageHeaderAccessor createAccessor(Message<?> message) {
		return new MessageHeaderAccessor(message);
	}


	// Configuration properties

	/**
	 * 默认情况下, 调用{@link #getMessageHeaders()}时, {@code "this"} {@code MessageHeaderAccessor}实例不能再用于修改底层消息header,
	 * 并且返回的{@code MessageHeaders}是不可变的.
	 * <p>但是, 如果将其设置为{@code true}, 则返回的 (底层) {@code MessageHeaders}实例仍然是可变的.
	 * 要进行进一步修改, 继续使用相同的访问者实例或重新获取它通过:<br>
	 * {@link MessageHeaderAccessor#getAccessor(Message, Class) MessageHeaderAccessor.getAccessor(Message, Class)}
	 * <p>修改完成后, 使用{@link #setImmutable()}来防止进一步的更改.
	 * 此机制的预期用例是在单个线程内初始化Message.
	 * <p>默认{@code false}.
	 */
	public void setLeaveMutable(boolean leaveMutable) {
		Assert.state(this.headers.isMutable(), "Already immutable");
		this.leaveMutable = leaveMutable;
	}

	/**
	 * 默认情况下, 调用{@link #getMessageHeaders()}时, {@code "this"} {@code MessageHeaderAccessor}实例不能再用于修改底层消息header.
	 * 但是, 如果使用{@link #setLeaveMutable(boolean)}, 则必须使用此方法明确指示何时不应再修改{@code MessageHeaders}实例.
	 */
	public void setImmutable() {
		this.headers.setImmutable();
	}

	/**
	 * 是否仍可以修改底层 header.
	 */
	public boolean isMutable() {
		return this.headers.isMutable();
	}

	/**
	 * 将底层消息header标记为已修改.
	 * 
	 * @param modified 通常是{@code true}, 或{@code false}重置标志
	 */
	protected void setModified(boolean modified) {
		this.modified = modified;
	}

	/**
	 * 检查底层消息header是否标记为已修改.
	 * 
	 * @return {@code true}如果已设置标志, 否则{@code false}
	 */
	public boolean isModified() {
		return this.modified;
	}

	/**
	 * 包级私有, 可以自动添加{@link org.springframework.messaging.MessageHeaders#TIMESTAMP} header.
	 * <p>默认{@code false}.
	 */
	void setEnableTimestamp(boolean enableTimestamp) {
		this.enableTimestamp = enableTimestamp;
	}

	/**
	 * 用于配置要使用的IdGenerator策略.
	 * <p>默认不设置此属性, 使用{@link org.springframework.messaging.MessageHeaders}中的默认IdGenerator.
	 */
	void setIdGenerator(IdGenerator idGenerator) {
		this.idGenerator = idGenerator;
	}


	// Accessors for the resulting MessageHeaders

	/**
	 * 返回底层{@code MessageHeaders}实例.
	 * <p>除非{@link #setLeaveMutable(boolean)}设置为{@code true}, 否则在此调用之后,
	 * header是不可变的, 并且此访问器不能再修改它们.
	 * <p>如果多次调用, 此方法始终返回相同的{@code MessageHeaders}实例.
	 * 要获取底层header的副本, 使用{@link #toMessageHeaders()}或{@link #toMap()}.
	 */
	public MessageHeaders getMessageHeaders() {
		if (!this.leaveMutable) {
			setImmutable();
		}
		return this.headers;
	}

	/**
	 * 返回底层header值的副本.
	 * <p>可以多次调用此方法, 并在每个新调用返回当前header值的新副本之间进行修改.
	 */
	public MessageHeaders toMessageHeaders() {
		return new MessageHeaders(this.headers);
	}

	/**
	 * 返回底层header值的副本.
	 * <p>可以多次调用此方法, 并在每个新调用返回当前header值的新副本之间进行修改.
	 */
	public Map<String, Object> toMap() {
		return new HashMap<String, Object>(this.headers);
	}


	// Generic header accessors

	/**
	 * 检索具有给定名称的header的值.
	 * 
	 * @param headerName header的名称
	 * 
	 * @return 关联的值, 或{@code null}
	 */
	public Object getHeader(String headerName) {
		return this.headers.get(headerName);
	}

	/**
	 * 设置给定header名称的值.
	 * <p>如果提供的值为{@code null}, 则将删除header.
	 */
	public void setHeader(String name, Object value) {
		if (isReadOnly(name)) {
			throw new IllegalArgumentException("'" + name + "' header is read-only");
		}
		verifyType(name, value);
		if (value != null) {
			// Modify header if necessary
			if (!ObjectUtils.nullSafeEquals(value, getHeader(name))) {
				this.modified = true;
				this.headers.getRawHeaders().put(name, value);
			}
		}
		else {
			// Remove header if available
			if (this.headers.containsKey(name)) {
				this.modified = true;
				this.headers.getRawHeaders().remove(name);
			}
		}
	}

	protected void verifyType(String headerName, Object headerValue) {
		if (headerName != null && headerValue != null) {
			if (MessageHeaders.ERROR_CHANNEL.equals(headerName) ||
					MessageHeaders.REPLY_CHANNEL.endsWith(headerName)) {
				if (!(headerValue instanceof MessageChannel || headerValue instanceof String)) {
					throw new IllegalArgumentException(
							"'" + headerName + "' header value must be a MessageChannel or String");
				}
			}
		}
	}

	/**
	 * 仅当header名称尚未与值关联时, 才设置给定header名称的值.
	 */
	public void setHeaderIfAbsent(String name, Object value) {
		if (getHeader(name) == null) {
			setHeader(name, value);
		}
	}

	/**
	 * 删除给定header名称的值.
	 */
	public void removeHeader(String headerName) {
		if (StringUtils.hasLength(headerName) && !isReadOnly(headerName)) {
			setHeader(headerName, null);
		}
	}

	/**
	 * 删除通过'headerPatterns'数组提供的所有header.
	 * <p>顾名思义, 数组可能包含header名称的简单匹配模式.
	 * 支持的模式: "xxx*", "*xxx", "*xxx*" and "xxx*yyy".
	 */
	public void removeHeaders(String... headerPatterns) {
		List<String> headersToRemove = new ArrayList<String>();
		for (String pattern : headerPatterns) {
			if (StringUtils.hasLength(pattern)){
				if (pattern.contains("*")){
					headersToRemove.addAll(getMatchingHeaderNames(pattern, this.headers));
				}
				else {
					headersToRemove.add(pattern);
				}
			}
		}
		for (String headerToRemove : headersToRemove) {
			removeHeader(headerToRemove);
		}
	}

	private List<String> getMatchingHeaderNames(String pattern, Map<String, Object> headers) {
		List<String> matchingHeaderNames = new ArrayList<String>();
		if (headers != null) {
			for (String key : headers.keySet()) {
				if (PatternMatchUtils.simpleMatch(pattern, key)) {
					matchingHeaderNames.add(key);
				}
			}
		}
		return matchingHeaderNames;
	}

	/**
	 * 从提供的Map复制名称-值对.
	 * <p>此操作将覆盖任何现有值. 使用{@link #copyHeadersIfAbsent(Map)}来避免覆盖值.
	 */
	public void copyHeaders(Map<String, ?> headersToCopy) {
		if (headersToCopy != null) {
			for (Map.Entry<String, ?> entry : headersToCopy.entrySet()) {
				if (!isReadOnly(entry.getKey())) {
					setHeader(entry.getKey(), entry.getValue());
				}
			}
		}
	}

	/**
	 * 从提供的Map复制名称-值对.
	 * <p>此操作<em>不会</em>覆盖任何现有值.
	 */
	public void copyHeadersIfAbsent(Map<String, ?> headersToCopy) {
		if (headersToCopy != null) {
			for (Map.Entry<String, ?> entry : headersToCopy.entrySet()) {
				if (!isReadOnly(entry.getKey())) {
					setHeaderIfAbsent(entry.getKey(), entry.getValue());
				}
			}
		}
	}

	protected boolean isReadOnly(String headerName) {
		return (MessageHeaders.ID.equals(headerName) || MessageHeaders.TIMESTAMP.equals(headerName));
	}


	// Specific header accessors

	public UUID getId() {
		Object value = getHeader(MessageHeaders.ID);
		if (value == null) {
			return null;
		}
		return (value instanceof UUID ? (UUID) value : UUID.fromString(value.toString()));
	}

	public Long getTimestamp() {
		Object value = getHeader(MessageHeaders.TIMESTAMP);
		if (value == null) {
			return null;
		}
		return (value instanceof Long ? (Long) value : Long.parseLong(value.toString()));
	}

	public void setContentType(MimeType contentType) {
		setHeader(MessageHeaders.CONTENT_TYPE, contentType);
	}

	public MimeType getContentType() {
		Object value = getHeader(MessageHeaders.CONTENT_TYPE);
		if (value == null) {
			return null;
		}
		return (value instanceof MimeType ? (MimeType) value : MimeType.valueOf(value.toString()));
	}

	public void setReplyChannelName(String replyChannelName) {
		setHeader(MessageHeaders.REPLY_CHANNEL, replyChannelName);
	}

	public void setReplyChannel(MessageChannel replyChannel) {
		setHeader(MessageHeaders.REPLY_CHANNEL, replyChannel);
	}

	public Object getReplyChannel() {
        return getHeader(MessageHeaders.REPLY_CHANNEL);
    }

	public void setErrorChannelName(String errorChannelName) {
		setHeader(MessageHeaders.ERROR_CHANNEL, errorChannelName);
	}

	public void setErrorChannel(MessageChannel errorChannel) {
		setHeader(MessageHeaders.ERROR_CHANNEL, errorChannel);
	}

    public Object getErrorChannel() {
        return getHeader(MessageHeaders.ERROR_CHANNEL);
    }


	// Log message stuff

	/**
	 * 返回简明消息以进行日志记录.
	 * 
	 * @param payload 与header对应的有效负载.
	 * 
	 * @return 消息
	 */
	public String getShortLogMessage(Object payload) {
		return "headers=" + this.headers.toString() + getShortPayloadLogMessage(payload);
	}

	/**
	 * 返回更详细的消息以进行日志记录.
	 * 
	 * @param payload 与header对应的有效负载.
	 * 
	 * @return 消息
	 */
	public String getDetailedLogMessage(Object payload) {
		return "headers=" + this.headers.toString() + getDetailedPayloadLogMessage(payload);
	}

	protected String getShortPayloadLogMessage(Object payload) {
		if (payload instanceof String) {
			String payloadText = (String) payload;
			return (payloadText.length() < 80) ?
				" payload=" + payloadText :
				" payload=" + payloadText.substring(0, 80) + "...(truncated)";
		}
		else if (payload instanceof byte[]) {
			byte[] bytes = (byte[]) payload;
			if (isReadableContentType()) {
				Charset charset = getContentType().getCharset();
				charset = (charset != null ? charset : DEFAULT_CHARSET);
				return (bytes.length < 80) ?
						" payload=" + new String(bytes, charset) :
						" payload=" + new String(Arrays.copyOf(bytes, 80), charset) + "...(truncated)";
			}
			else {
				return " payload=byte[" + bytes.length + "]";
			}
		}
		else {
			String payloadText = payload.toString();
			return (payloadText.length() < 80) ?
					" payload=" + payloadText :
					" payload=" + ObjectUtils.identityToString(payload);
		}
	}

	protected String getDetailedPayloadLogMessage(Object payload) {
		if (payload instanceof String) {
			return " payload=" + payload;
		}
		else if (payload instanceof byte[]) {
			byte[] bytes = (byte[]) payload;
			if (isReadableContentType()) {
				Charset charset = getContentType().getCharset();
				charset = (charset != null ? charset : DEFAULT_CHARSET);
				return " payload=" + new String(bytes, charset);
			}
			else {
				return " payload=byte[" + bytes.length + "]";
			}
		}
		else {
			return " payload=" + payload;
		}
	}

	protected boolean isReadableContentType() {
		MimeType contentType = getContentType();
		for (MimeType mimeType : READABLE_MIME_TYPES) {
			if (mimeType.includes(contentType)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " [headers=" + this.headers + "]";
	}


	// Static factory methods

	/**
	 * 返回用于创建给定{@code Message} header的原始{@code MessageHeaderAccessor},
	 * 如果不可用或者其类型与所需类型不匹配, 则返回{@code null}.
	 * <p>这适用于强烈期望存在访问器 (随后是断言) 或者创建访问器的情况.
	 * 
	 * @param message 要获取访问器的消息
	 * @param requiredType 所需的访问器类型 (或{@code null}不限制)
	 * 
	 * @return 指定类型的访问器实例, 或{@code null}
	 */
	public static <T extends MessageHeaderAccessor> T getAccessor(Message<?> message, Class<T> requiredType) {
		return getAccessor(message.getHeaders(), requiredType);
	}

	/**
	 * 使用{@code MessageHeaders}实例而不是{@code Message}的{@link #getAccessor(org.springframework.messaging.Message, Class)}的变体.
	 * <p>这适用于尚未创建完整消息的情况.
	 * 
	 * @param messageHeaders 要获取访问器的消息header
	 * @param requiredType 所需的访问器类型 (或{@code null}不限制)
	 * 
	 * @return 指定类型的访问器实例, 或{@code null}
	 */
	@SuppressWarnings("unchecked")
	public static <T extends MessageHeaderAccessor> T getAccessor(
			MessageHeaders messageHeaders, Class<T> requiredType) {

		if (messageHeaders instanceof MutableMessageHeaders) {
			MutableMessageHeaders mutableHeaders = (MutableMessageHeaders) messageHeaders;
			MessageHeaderAccessor headerAccessor = mutableHeaders.getAccessor();
			if (requiredType == null || requiredType.isInstance(headerAccessor))  {
				return (T) headerAccessor;
			}
		}
		return null;
	}

	/**
	 * 返回给定消息的可变{@code MessageHeaderAccessor}, 尝试匹配用于创建消息header的访问器类型,
	 * 或者使用{@code MessageHeaderAccessor}实例包装消息.
	 * <p>这适用于需要在通用代码中更新header, 同时保留用于下游处理的访问器类型的情况.
	 * 
	 * @return 所需类型的访问器 (never {@code null})
	 */
	public static MessageHeaderAccessor getMutableAccessor(Message<?> message) {
		if (message.getHeaders() instanceof MutableMessageHeaders) {
			MutableMessageHeaders mutableHeaders = (MutableMessageHeaders) message.getHeaders();
			MessageHeaderAccessor accessor = mutableHeaders.getAccessor();
			if (accessor != null) {
				return (accessor.isMutable() ? accessor : accessor.createAccessor(message));
			}
		}
		return new MessageHeaderAccessor(message);
	}


	@SuppressWarnings("serial")
	private class MutableMessageHeaders extends MessageHeaders {

		private boolean mutable = true;

		public MutableMessageHeaders(Map<String, Object> headers) {
			super(headers, MessageHeaders.ID_VALUE_NONE, -1L);
		}

		@Override
		public Map<String, Object> getRawHeaders() {
			Assert.state(this.mutable, "Already immutable");
			return super.getRawHeaders();
		}

		public void setImmutable() {
			if (!this.mutable) {
				return;
			}

			if (getId() == null) {
				IdGenerator idGenerator = (MessageHeaderAccessor.this.idGenerator != null ?
						MessageHeaderAccessor.this.idGenerator : MessageHeaders.getIdGenerator());
				UUID id = idGenerator.generateId();
				if (id != null && id != MessageHeaders.ID_VALUE_NONE) {
					getRawHeaders().put(ID, id);
				}
			}

			if (getTimestamp() == null) {
				if (MessageHeaderAccessor.this.enableTimestamp) {
					getRawHeaders().put(TIMESTAMP, System.currentTimeMillis());
				}
			}

			this.mutable = false;
		}

		public boolean isMutable() {
			return this.mutable;
		}

		public MessageHeaderAccessor getAccessor() {
			return MessageHeaderAccessor.this;
		}

		protected Object writeReplace() {
			// 序列化为常规MessageHeaders (没有MessageHeaderAccessor引用)
			return new MessageHeaders(this);
		}
	}
}
