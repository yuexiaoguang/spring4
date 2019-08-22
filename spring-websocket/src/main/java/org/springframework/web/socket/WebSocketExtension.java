package org.springframework.web.socket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.util.StringUtils;

/**
 * 表示RFC 6455中定义的WebSocket扩展.
 * WebSocket扩展为WebSocket协议添加了协议功能.
 * 会话中使用的扩展在握手阶段协商如下:
 * <ul>
 * <li>客户端可能会要求HTTP握手请求中的特定扩展</li>
 * <li>服务器使用当前会话中使用的最终扩展列表进行响应</li>
 * </ul>
 *
 * <p>WebSocket Extension HTTP header可能包含参数并遵循
 * <a href="http://tools.ietf.org/html/rfc7230#section-3.2">RFC 7230 section 3.2</a></p>
 *
 * <p>请注意, HTTP header中的扩展顺序定义了它们的执行顺序,
 * e.g. 扩展"foo, bar" 将被执行为"bar(foo(message))".</p>
 */
public class WebSocketExtension {

	private final String name;

	private final Map<String, String> parameters;


	/**
	 * @param name 扩展的名称
	 */
	public WebSocketExtension(String name) {
		this(name, null);
	}

	/**
	 * @param name 扩展的名称
	 * @param parameters 参数
	 */
	public WebSocketExtension(String name, Map<String, String> parameters) {
		Assert.hasLength(name, "Extension name must not be empty");
		this.name = name;
		if (!CollectionUtils.isEmpty(parameters)) {
			Map<String, String> map = new LinkedCaseInsensitiveMap<String>(parameters.size(), Locale.ENGLISH);
			map.putAll(parameters);
			this.parameters = Collections.unmodifiableMap(map);
		}
		else {
			this.parameters = Collections.emptyMap();
		}
	}


	/**
	 * 返回扩展的名称 (never {@code null) or empty}.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * 返回扩展的参数 (never {@code null}).
	 */
	public Map<String, String> getParameters() {
		return this.parameters;
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (other == null || getClass() != other.getClass()) {
			return false;
		}
		WebSocketExtension otherExt = (WebSocketExtension) other;
		return (this.name.equals(otherExt.name) && this.parameters.equals(otherExt.parameters));
	}

	@Override
	public int hashCode() {
		return this.name.hashCode() * 31 + this.parameters.hashCode();
	}

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		str.append(this.name);
		for (Map.Entry<String, String> entry : this.parameters.entrySet()) {
			str.append(';');
			str.append(entry.getKey());
			str.append('=');
			str.append(entry.getValue());
		}
		return str.toString();
	}


	/**
	 * 将给定的逗号分隔的字符串解析为{@code WebSocketExtension}对象列表.
	 * <p>此方法可用于解析"Sec-WebSocket-Extension" header.
	 * 
	 * @param extensions 要解析的字符串
	 * 
	 * @return 扩展列表
	 * @throws IllegalArgumentException 如果字符串无法解析
	 */
	public static List<WebSocketExtension> parseExtensions(String extensions) {
		if (StringUtils.hasText(extensions)) {
			String[] tokens = StringUtils.tokenizeToStringArray(extensions, ",");
			List<WebSocketExtension> result = new ArrayList<WebSocketExtension>(tokens.length);
			for (String token : tokens) {
				result.add(parseExtension(token));
			}
			return result;
		}
		else {
			return Collections.emptyList();
		}
	}

	private static WebSocketExtension parseExtension(String extension) {
		if (extension.contains(",")) {
			throw new IllegalArgumentException("Expected single extension value: [" + extension + "]");
		}
		String[] parts = StringUtils.tokenizeToStringArray(extension, ";");
		String name = parts[0].trim();

		Map<String, String> parameters = null;
		if (parts.length > 1) {
			parameters = new LinkedHashMap<String, String>(parts.length - 1);
			for (int i = 1; i < parts.length; i++) {
				String parameter = parts[i];
				int eqIndex = parameter.indexOf('=');
				if (eqIndex != -1) {
					String attribute = parameter.substring(0, eqIndex);
					String value = parameter.substring(eqIndex + 1, parameter.length());
					parameters.put(attribute, value);
				}
			}
		}

		return new WebSocketExtension(name, parameters);
	}

}
