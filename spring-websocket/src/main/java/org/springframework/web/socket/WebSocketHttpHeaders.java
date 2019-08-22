package org.springframework.web.socket;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpHeaders;
import org.springframework.util.CollectionUtils;

/**
 * {@link org.springframework.http.HttpHeaders}变体, 它增加了对WebSocket规范 RFC 6455定义的HTTP header的支持.
 */
public class WebSocketHttpHeaders extends HttpHeaders {

	public static final String SEC_WEBSOCKET_ACCEPT = "Sec-WebSocket-Accept";

	public static final String SEC_WEBSOCKET_EXTENSIONS = "Sec-WebSocket-Extensions";

	public static final String SEC_WEBSOCKET_KEY = "Sec-WebSocket-Key";

	public static final String SEC_WEBSOCKET_PROTOCOL = "Sec-WebSocket-Protocol";

	public static final String SEC_WEBSOCKET_VERSION = "Sec-WebSocket-Version";

	private static final long serialVersionUID = -6644521016187828916L;


	private final HttpHeaders headers;


	public WebSocketHttpHeaders() {
		this(new HttpHeaders(), false);
	}

	/**
	 * 创建一个包装给定的预先存在的HttpHeaders的实例, 并将所有更改传播给它.
	 * 
	 * @param headers 要包装的HTTP header
	 */
	public WebSocketHttpHeaders(HttpHeaders headers) {
		this(headers, false);
	}

	/**
	 * 创建只读{@code WebSocketHttpHeader}实例.
	 */
	private WebSocketHttpHeaders(HttpHeaders headers, boolean readOnly) {
		this.headers = readOnly ? HttpHeaders.readOnlyHttpHeaders(headers) : headers;
	}

	/**
	 * 返回只能读取而不能写入的{@code WebSocketHttpHeaders}对象.
	 */
	public static WebSocketHttpHeaders readOnlyWebSocketHttpHeaders(WebSocketHttpHeaders headers) {
		return new WebSocketHttpHeaders(headers, true);
	}


	/**
	 * 设置{@code Sec-WebSocket-Accept} header的值.
	 * 
	 * @param secWebSocketAccept header的值
	 */
	public void setSecWebSocketAccept(String secWebSocketAccept) {
		set(SEC_WEBSOCKET_ACCEPT, secWebSocketAccept);
	}

	/**
	 * 返回{@code Sec-WebSocket-Accept} header的值.
	 * 
	 * @return header的值
	 */
	public String getSecWebSocketAccept() {
		return getFirst(SEC_WEBSOCKET_ACCEPT);
	}

	/**
	 * 返回{@code Sec-WebSocket-Extensions} header的值.
	 * 
	 * @return header的值
	 */
	public List<WebSocketExtension> getSecWebSocketExtensions() {
		List<String> values = get(SEC_WEBSOCKET_EXTENSIONS);
		if (CollectionUtils.isEmpty(values)) {
			return Collections.emptyList();
		}
		else {
			List<WebSocketExtension> result = new ArrayList<WebSocketExtension>(values.size());
			for (String value : values) {
				result.addAll(WebSocketExtension.parseExtensions(value));
			}
			return result;
		}
	}

	/**
	 * 设置{@code Sec-WebSocket-Extensions} header的值.
	 * 
	 * @param extensions header的值
	 */
	public void setSecWebSocketExtensions(List<WebSocketExtension> extensions) {
		List<String> result = new ArrayList<String>(extensions.size());
		for (WebSocketExtension extension : extensions) {
			result.add(extension.toString());
		}
		set(SEC_WEBSOCKET_EXTENSIONS, toCommaDelimitedString(result));
	}

	/**
	 * 设置{@code Sec-WebSocket-Key} header的值.
	 * 
	 * @param secWebSocketKey header的值
	 */
	public void setSecWebSocketKey(String secWebSocketKey) {
		set(SEC_WEBSOCKET_KEY, secWebSocketKey);
	}

	/**
	 * 返回{@code Sec-WebSocket-Key} header的值.
	 * 
	 * @return header的值
	 */
	public String getSecWebSocketKey() {
		return getFirst(SEC_WEBSOCKET_KEY);
	}

	/**
	 * 设置{@code Sec-WebSocket-Protocol} header的值.
	 * 
	 * @param secWebSocketProtocol header的值
	 */
	public void setSecWebSocketProtocol(String secWebSocketProtocol) {
		if (secWebSocketProtocol != null) {
			set(SEC_WEBSOCKET_PROTOCOL, secWebSocketProtocol);
		}
	}

	/**
	 * 设置{@code Sec-WebSocket-Protocol} header的值.
	 * 
	 * @param secWebSocketProtocols header的值
	 */
	public void setSecWebSocketProtocol(List<String> secWebSocketProtocols) {
		set(SEC_WEBSOCKET_PROTOCOL, toCommaDelimitedString(secWebSocketProtocols));
	}

	/**
	 * 返回{@code Sec-WebSocket-Key} header的值.
	 * 
	 * @return header的值
	 */
	public List<String> getSecWebSocketProtocol() {
		List<String> values = get(SEC_WEBSOCKET_PROTOCOL);
		if (CollectionUtils.isEmpty(values)) {
			return Collections.emptyList();
		}
		else if (values.size() == 1) {
			return getValuesAsList(SEC_WEBSOCKET_PROTOCOL);
		}
		else {
			return values;
		}
	}

	/**
	 * 设置{@code Sec-WebSocket-Version} header的值.
	 * 
	 * @param secWebSocketVersion header的值
	 */
	public void setSecWebSocketVersion(String secWebSocketVersion) {
		set(SEC_WEBSOCKET_VERSION, secWebSocketVersion);
	}

	/**
	 * 返回{@code Sec-WebSocket-Version} header的值.
	 * 
	 * @return header的值
	 */
	public String getSecWebSocketVersion() {
		return getFirst(SEC_WEBSOCKET_VERSION);
	}


	// Single string methods

	/**
	 * 返回给定header名称的第一个header值.
	 * 
	 * @param headerName header名称
	 * 
	 * @return 第一个header值; 或{@code null}
	 */
	@Override
	public String getFirst(String headerName) {
		return this.headers.getFirst(headerName);
	}

	/**
	 * 在给定名称下添加给定的单个header值.
	 * 
	 * @param headerName  header名称
	 * @param headerValue header的值
	 * 
	 * @throws UnsupportedOperationException 如果不支持添加header
	 */
	@Override
	public void add(String headerName, String headerValue) {
		this.headers.add(headerName, headerValue);
	}

	/**
	 * 在给定名称下设置给定的单个header值.
	 * 
	 * @param headerName  header名称
	 * @param headerValue header的值
	 * 
	 * @throws UnsupportedOperationException 如果不支持添加header
	 */
	@Override
	public void set(String headerName, String headerValue) {
		this.headers.set(headerName, headerValue);
	}

	@Override
	public void setAll(Map<String, String> values) {
		this.headers.setAll(values);
	}

	@Override
	public Map<String, String> toSingleValueMap() {
		return this.headers.toSingleValueMap();
	}

	// Map implementation

	@Override
	public int size() {
		return this.headers.size();
	}

	@Override
	public boolean isEmpty() {
		return this.headers.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return this.headers.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return this.headers.containsValue(value);
	}

	@Override
	public List<String> get(Object key) {
		return this.headers.get(key);
	}

	@Override
	public List<String> put(String key, List<String> value) {
		return this.headers.put(key, value);
	}

	@Override
	public List<String> remove(Object key) {
		return this.headers.remove(key);
	}

	@Override
	public void putAll(Map<? extends String, ? extends List<String>> m) {
		this.headers.putAll(m);
	}

	@Override
	public void clear() {
		this.headers.clear();
	}

	@Override
	public Set<String> keySet() {
		return this.headers.keySet();
	}

	@Override
	public Collection<List<String>> values() {
		return this.headers.values();
	}

	@Override
	public Set<Entry<String, List<String>>> entrySet() {
		return this.headers.entrySet();
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof WebSocketHttpHeaders)) {
			return false;
		}
		WebSocketHttpHeaders otherHeaders = (WebSocketHttpHeaders) other;
		return this.headers.equals(otherHeaders.headers);
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
