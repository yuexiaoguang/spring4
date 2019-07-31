package org.springframework.http.client.support;

import java.net.URI;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.util.Assert;

/**
 * 提供{@link HttpRequest}接口的便捷实现, 可以重写该接口以适配请求.
 *
 * <p>这些方法默认调用包装的请求对象.
 */
public class HttpRequestWrapper implements HttpRequest {

	private final HttpRequest request;


	/**
	 * @param request 要包装的请求对象
	 */
	public HttpRequestWrapper(HttpRequest request) {
		Assert.notNull(request, "HttpRequest must not be null");
		this.request = request;
	}


	/**
	 * 返回包装的请求.
	 */
	public HttpRequest getRequest() {
		return this.request;
	}

	/**
	 * 返回包装的请求的方法.
	 */
	@Override
	public HttpMethod getMethod() {
		return this.request.getMethod();
	}

	/**
	 * 返回包装的请求的URI.
	 */
	@Override
	public URI getURI() {
		return this.request.getURI();
	}

	/**
	 * 返回包装的请求的header.
	 */
	@Override
	public HttpHeaders getHeaders() {
		return this.request.getHeaders();
	}

}
