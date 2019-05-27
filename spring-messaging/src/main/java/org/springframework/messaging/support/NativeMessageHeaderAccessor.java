package org.springframework.messaging.support;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;

/**
 * {@link MessageHeaderAccessor}的扩展, 它还存储并提供来自外部源的消息header的读/写访问
 * -- e.g. 创建Spring {@link Message}以表示从STOMP客户端或消息代理接收的STOMP消息.
 * 本地消息 header保存在键{@link #NATIVE_HEADERS}下的{@code Map<String, List<String>>}中.
 *
 * <p>此类不是为了直接使用, 而是希望通过特定于协议的子类间接使用, 例如
 * {@link org.springframework.messaging.simp.stomp.StompHeaderAccessor StompHeaderAccessor}.
 * 这些子类可以提供工厂方法来将消息header从外部消息传递源 (e.g. STOMP)转换为Spring {@link Message} header,
 * 反之以将Spring {@link Message} header转换为消息以发送到外部源.
 */
public class NativeMessageHeaderAccessor extends MessageHeaderAccessor {

	public static final String NATIVE_HEADERS = "nativeHeaders";


	protected NativeMessageHeaderAccessor() {
		this((Map<String, List<String>>) null);
	}

	/**
	 * @param nativeHeaders 用于创建消息的本机header (may be {@code null})
	 */
	protected NativeMessageHeaderAccessor(Map<String, List<String>> nativeHeaders) {
		if (!CollectionUtils.isEmpty(nativeHeaders)) {
			setHeader(NATIVE_HEADERS, new LinkedMultiValueMap<String, String>(nativeHeaders));
		}
	}

	/**
	 * 接受要复制的现有消息的header.
	 */
	protected NativeMessageHeaderAccessor(Message<?> message) {
		super(message);
		if (message != null) {
			@SuppressWarnings("unchecked")
			Map<String, List<String>> map = (Map<String, List<String>>) getHeader(NATIVE_HEADERS);
			if (map != null) {
				// 强制删除, 因为setHeader检查是否相等
				removeHeader(NATIVE_HEADERS);
				setHeader(NATIVE_HEADERS, new LinkedMultiValueMap<String, String>(map));
			}
		}
	}

	@SuppressWarnings("unchecked")
	private Map<String, List<String>> getNativeHeaders() {
		return (Map<String, List<String>>) getHeader(NATIVE_HEADERS);
	}

	/**
	 * 返回本机header值或空Map的副本.
	 */
	public Map<String, List<String>> toNativeHeaderMap() {
		Map<String, List<String>> map = getNativeHeaders();
		return (map != null ? new LinkedMultiValueMap<String, String>(map) : Collections.<String, List<String>>emptyMap());
	}

	@Override
	public void setImmutable() {
		if (isMutable()) {
			Map<String, List<String>> map = getNativeHeaders();
			if (map != null) {
				// 强制删除, 因为setHeader检查是否相等
				removeHeader(NATIVE_HEADERS);
				setHeader(NATIVE_HEADERS, Collections.<String, List<String>>unmodifiableMap(map));
			}
			super.setImmutable();
		}
	}

	/**
	 * 本机header Map是否包含给定header名称.
	 */
	public boolean containsNativeHeader(String headerName) {
		Map<String, List<String>> map = getNativeHeaders();
		return (map != null && map.containsKey(headerName));
	}

	/**
	 * @return 指定本机header的所有值或{@code null}.
	 */
	public List<String> getNativeHeader(String headerName) {
		Map<String, List<String>> map = getNativeHeaders();
		return (map != null ? map.get(headerName) : null);
	}

	/**
	 * @return {@code null}的指定本机header的第一个值.
	 */
	public String getFirstNativeHeader(String headerName) {
		Map<String, List<String>> map = getNativeHeaders();
		if (map != null) {
			List<String> values = map.get(headerName);
			if (values != null) {
				return values.get(0);
			}
		}
		return null;
	}

	/**
	 * 设置指定的本机header值替换现有值.
	 */
	public void setNativeHeader(String name, String value) {
		Assert.state(isMutable(), "Already immutable");
		Map<String, List<String>> map = getNativeHeaders();
		if (value == null) {
			if (map != null && map.get(name) != null) {
				setModified(true);
				map.remove(name);
			}
			return;
		}
		if (map == null) {
			map = new LinkedMultiValueMap<String, String>(4);
			setHeader(NATIVE_HEADERS, map);
		}
		List<String> values = new LinkedList<String>();
		values.add(value);
		if (!ObjectUtils.nullSafeEquals(values, getHeader(name))) {
			setModified(true);
			map.put(name, values);
		}
	}

	/**
	 * 将指定的本机header值添加到现有值.
	 */
	public void addNativeHeader(String name, String value) {
		Assert.state(isMutable(), "Already immutable");
		if (value == null) {
			return;
		}
		Map<String, List<String>> nativeHeaders = getNativeHeaders();
		if (nativeHeaders == null) {
			nativeHeaders = new LinkedMultiValueMap<String, String>(4);
			setHeader(NATIVE_HEADERS, nativeHeaders);
		}
		List<String> values = nativeHeaders.get(name);
		if (values == null) {
			values = new LinkedList<String>();
			nativeHeaders.put(name, values);
		}
		values.add(value);
		setModified(true);
	}

	public void addNativeHeaders(MultiValueMap<String, String> headers) {
		if (headers == null) {
			return;
		}
		for (Map.Entry<String, List<String>> headerEntry : headers.entrySet()) {
			for (String value : headerEntry.getValue()) {
				addNativeHeader(headerEntry.getKey(), value);
			}
		}
	}

	public List<String> removeNativeHeader(String name) {
		Assert.state(isMutable(), "Already immutable");
		Map<String, List<String>> nativeHeaders = getNativeHeaders();
		if (nativeHeaders == null) {
			return null;
		}
		return nativeHeaders.remove(name);
	}

	@SuppressWarnings("unchecked")
	public static String getFirstNativeHeader(String headerName, Map<String, Object> headers) {
		Map<String, List<String>> map = (Map<String, List<String>>) headers.get(NATIVE_HEADERS);
		if (map != null) {
			List<String> values = map.get(headerName);
			if (values != null) {
				return values.get(0);
			}
		}
		return null;
	}

}
