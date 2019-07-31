package org.springframework.http;

import java.util.HashMap;
import java.util.Map;

/**
 * HTTP请求方法的Java 5枚举.
 * 旨在与{@link org.springframework.http.client.ClientHttpRequest}
 * 和{@link org.springframework.web.client.RestTemplate}一起使用.
 */
public enum HttpMethod {

	GET, HEAD, POST, PUT, PATCH, DELETE, OPTIONS, TRACE;


	private static final Map<String, HttpMethod> mappings = new HashMap<String, HttpMethod>(16);

	static {
		for (HttpMethod httpMethod : values()) {
			mappings.put(httpMethod.name(), httpMethod);
		}
	}


	/**
	 * 将给定的方法值解析为{@code HttpMethod}.
	 * 
	 * @param method 方法值
	 * 
	 * @return 相应的{@code HttpMethod}, 或{@code null}
	 */
	public static HttpMethod resolve(String method) {
		return (method != null ? mappings.get(method) : null);
	}


	/**
	 * 确定此{@code HttpMethod}是否与给定的方法值匹配.
	 * 
	 * @param method 方法值
	 * 
	 * @return {@code true}如果匹配, 否则{@code false}
	 */
	public boolean matches(String method) {
		return (this == resolve(method));
	}
}
