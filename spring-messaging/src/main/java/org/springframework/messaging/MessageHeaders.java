package org.springframework.messaging;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.AlternativeJdkIdGenerator;
import org.springframework.util.IdGenerator;

/**
 * {@link Message}的header.
 *
 * <p><b>IMPORTANT</b>: 这个类是不可变的.
 * 任何变异操作, 例如{@code put(..)}, {@code putAll(..)} 和其他操作都会抛出{@link UnsupportedOperationException}.
 * <p>但是, 子类可以通过{@link #getRawHeaders()}访问原始header.
 *
 * <p>创建消息header的一种方法是使用
 * {@link org.springframework.messaging.support.MessageBuilder MessageBuilder}:
 * <pre class="code">
 * MessageBuilder.withPayload("foo").setHeader("key1", "value1").setHeader("key2", "value2");
 * </pre>
 *
 * 第二个选项是创建{@link org.springframework.messaging.support.GenericMessage}将有效负载作为{@link Object}传递,
 * 将header作为{@link Map java.util.Map}传递:
 * <pre class="code">
 * Map headers = new HashMap();
 * headers.put("key1", "value1");
 * headers.put("key2", "value2");
 * new GenericMessage("foo", headers);
 * </pre>
 *
 * 第三种选择是使用{@link org.springframework.messaging.support.MessageHeaderAccessor}或其子类之一来创建特定类别的header.
 */
public class MessageHeaders implements Map<String, Object>, Serializable {

	public static final UUID ID_VALUE_NONE = new UUID(0,0);

	/**
	 * 消息ID的Key.
	 * 这是一个自动生成的UUID, 永远不应在header映射中显式设置,
	 * 但消息反序列化的情况除外, 因为正在恢复序列化消息生成的UUID.
	 */
	public static final String ID = "id";

	public static final String TIMESTAMP = "timestamp";

	public static final String CONTENT_TYPE = "contentType";

	public static final String REPLY_CHANNEL = "replyChannel";

	public static final String ERROR_CHANNEL = "errorChannel";


	private static final long serialVersionUID = 7035068984263400920L;

	private static final Log logger = LogFactory.getLog(MessageHeaders.class);

	private static final IdGenerator defaultIdGenerator = new AlternativeJdkIdGenerator();

	private static volatile IdGenerator idGenerator = null;


	private final Map<String, Object> headers;


	/**
	 * 还将添加{@link #ID}和{@link #TIMESTAMP} header, 覆盖任何现有值.
	 * 
	 * @param headers 要添加的header的映射
	 */
	public MessageHeaders(Map<String, Object> headers) {
		this(headers, null, null);
	}

	/**
	 * @param headers 要添加的header的映射
	 * @param id {@link #ID} header值
	 * @param timestamp {@link #TIMESTAMP} header值
	 */
	protected MessageHeaders(Map<String, Object> headers, UUID id, Long timestamp) {
		this.headers = (headers != null ? new HashMap<String, Object>(headers) : new HashMap<String, Object>());

		if (id == null) {
			this.headers.put(ID, getIdGenerator().generateId());
		}
		else if (id == ID_VALUE_NONE) {
			this.headers.remove(ID);
		}
		else {
			this.headers.put(ID, id);
		}

		if (timestamp == null) {
			this.headers.put(TIMESTAMP, System.currentTimeMillis());
		}
		else if (timestamp < 0) {
			this.headers.remove(TIMESTAMP);
		}
		else {
			this.headers.put(TIMESTAMP, timestamp);
		}
	}

	/**
	 * 复制构造函数, 允许忽略某些条目.
	 * 用于没有非可序列化条目的序列化.
	 * 
	 * @param original 要复制的MessageHeader
	 * @param keysToIgnore 要忽略的条目的键
	 */
	private MessageHeaders(MessageHeaders original, Set<String> keysToIgnore) {
		this.headers = new HashMap<String, Object>(original.headers.size() - keysToIgnore.size());
		for (Map.Entry<String, Object> entry : original.headers.entrySet()) {
			if (!keysToIgnore.contains(entry.getKey())) {
				this.headers.put(entry.getKey(), entry.getValue());
			}
		}
	}


	protected Map<String, Object> getRawHeaders() {
		return this.headers;
	}

	protected static IdGenerator getIdGenerator() {
		return (idGenerator != null ? idGenerator : defaultIdGenerator);
	}

	public UUID getId() {
		return get(ID, UUID.class);
	}

	public Long getTimestamp() {
		return get(TIMESTAMP, Long.class);
	}

	public Object getReplyChannel() {
		return get(REPLY_CHANNEL);
	}

	public Object getErrorChannel() {
		return get(ERROR_CHANNEL);
	}


	@SuppressWarnings("unchecked")
	public <T> T get(Object key, Class<T> type) {
		Object value = this.headers.get(key);
		if (value == null) {
			return null;
		}
		if (!type.isAssignableFrom(value.getClass())) {
			throw new IllegalArgumentException("Incorrect type specified for header '" +
					key + "'. Expected [" + type + "] but actual type is [" + value.getClass() + "]");
		}
		return (T) value;
	}


	// Delegating Map implementation

	public boolean containsKey(Object key) {
		return this.headers.containsKey(key);
	}

	public boolean containsValue(Object value) {
		return this.headers.containsValue(value);
	}

	public Set<Map.Entry<String, Object>> entrySet() {
		return Collections.unmodifiableMap(this.headers).entrySet();
	}

	public Object get(Object key) {
		return this.headers.get(key);
	}

	public boolean isEmpty() {
		return this.headers.isEmpty();
	}

	public Set<String> keySet() {
		return Collections.unmodifiableSet(this.headers.keySet());
	}

	public int size() {
		return this.headers.size();
	}

	public Collection<Object> values() {
		return Collections.unmodifiableCollection(this.headers.values());
	}


	// Unsupported Map operations

	/**
	 * 由于MessageHeader是不可变的, 因此对此方法的调用将导致{@link UnsupportedOperationException}.
	 */
	public Object put(String key, Object value) {
		throw new UnsupportedOperationException("MessageHeaders is immutable");
	}

	/**
	 * 由于MessageHeader是不可变的, 因此对此方法的调用将导致{@link UnsupportedOperationException}.
	 */
	public void putAll(Map<? extends String, ? extends Object> map) {
		throw new UnsupportedOperationException("MessageHeaders is immutable");
	}

	/**
	 * 由于MessageHeader是不可变的, 因此对此方法的调用将导致{@link UnsupportedOperationException}.
	 */
	public Object remove(Object key) {
		throw new UnsupportedOperationException("MessageHeaders is immutable");
	}

	/**
	 * 由于MessageHeader是不可变的, 因此对此方法的调用将导致{@link UnsupportedOperationException}.
	 */
	public void clear() {
		throw new UnsupportedOperationException("MessageHeaders is immutable");
	}


	// Serialization methods

	private void writeObject(ObjectOutputStream out) throws IOException {
		Set<String> keysToIgnore = new HashSet<String>();
		for (Map.Entry<String, Object> entry : this.headers.entrySet()) {
			if (!(entry.getValue() instanceof Serializable)) {
				keysToIgnore.add(entry.getKey());
			}
		}

		if (keysToIgnore.isEmpty()) {
			// 所有条目都是可序列化的 -> 序列化常规MessageHeader实例
			out.defaultWriteObject();
		}
		else {
			// 一些不可序列化的条目 -> 序列化一个临时的MessageHeader副本
			if (logger.isDebugEnabled()) {
				logger.debug("Ignoring non-serializable message headers: " + keysToIgnore);
			}
			out.writeObject(new MessageHeaders(this, keysToIgnore));
		}
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
	}


	// equals, hashCode, toString

	@Override
	public boolean equals(Object other) {
		return (this == other ||
				(other instanceof MessageHeaders && this.headers.equals(((MessageHeaders) other).headers)));
	}

	@Override
	public int hashCode() {
		return this.headers.hashCode();
	}

	@Override
	public String toString() {
		return this.headers.toString();
	}
}
