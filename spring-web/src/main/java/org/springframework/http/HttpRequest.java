package org.springframework.http;

import java.net.URI;

/**
 * 表示HTTP请求消息, 由{@linkplain #getMethod() 方法}和 {@linkplain #getURI() uri}组成.
 */
public interface HttpRequest extends HttpMessage {

	/**
	 * 返回请求的HTTP方法.
	 * 
	 * @return HttpMethod枚举值, 或{@code null} (e.g. 在非标准HTTP方法的情况下)
	 */
	HttpMethod getMethod();

	/**
	 * 返回请求的URI (包括查询字符串, 但只有在URI表示形式良好的情况下).
	 * 
	 * @return 请求的URI (never {@code null})
	 */
	URI getURI();

}
