package org.springframework.test.web.client.response;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.util.Assert;

/**
 * 带有构建器样式方法的{@code ResponseCreator}, 用于添加响应详细信息.
 */
public class DefaultResponseCreator implements ResponseCreator {

	private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");


	private HttpStatus statusCode;

	private byte[] content;

	private Resource contentResource;

	private final HttpHeaders headers = new HttpHeaders();


	/**
	 * 在{@link MockRestResponseCreators}中使用静态工厂方法.
	 */
	protected DefaultResponseCreator(HttpStatus statusCode) {
		Assert.notNull(statusCode, "HttpStatus must not be null");
		this.statusCode = statusCode;
	}


	/**
	 * 将主体设置为UTF-8字符串.
	 */
	public DefaultResponseCreator body(String content) {
		this.content = content.getBytes(UTF8_CHARSET);
		return this;
	}

	/**
	 * 将主体设置为字节数组.
	 */
	public DefaultResponseCreator body(byte[] content) {
		this.content = content;
		return this;
	}

	/**
	 * 将正文设置为{@link Resource}.
	 */
	public DefaultResponseCreator body(Resource resource) {
		this.contentResource = resource;
		return this;
	}

	/**
	 * 设置{@code Content-Type} header.
	 */
	public DefaultResponseCreator contentType(MediaType mediaType) {
		if (mediaType != null) {
			this.headers.setContentType(mediaType);
		}
		return this;
	}

	/**
	 * 设置{@code Location} header.
	 */
	public DefaultResponseCreator location(URI location) {
		this.headers.setLocation(location);
		return this;
	}

	/**
	 * 复制所有header.
	 */
	public DefaultResponseCreator headers(HttpHeaders headers) {
		for (String headerName : headers.keySet()) {
			for (String headerValue : headers.get(headerName)) {
				this.headers.add(headerName, headerValue);
			}
		}
		return this;
	}


	@Override
	public ClientHttpResponse createResponse(ClientHttpRequest request) throws IOException {
		MockClientHttpResponse response;
		if (this.contentResource != null) {
			InputStream stream = this.contentResource.getInputStream();
			response = new MockClientHttpResponse(stream, this.statusCode);
		}
		else {
			response = new MockClientHttpResponse(this.content, this.statusCode);
		}
		response.getHeaders().putAll(this.headers);
		return response;
	}

}
