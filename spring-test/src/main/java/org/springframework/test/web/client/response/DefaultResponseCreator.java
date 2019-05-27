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
 * A {@code ResponseCreator} with builder-style methods for adding response details.
 */
public class DefaultResponseCreator implements ResponseCreator {

	private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");


	private HttpStatus statusCode;

	private byte[] content;

	private Resource contentResource;

	private final HttpHeaders headers = new HttpHeaders();


	/**
	 * Protected constructor.
	 * Use static factory methods in {@link MockRestResponseCreators}.
	 */
	protected DefaultResponseCreator(HttpStatus statusCode) {
		Assert.notNull(statusCode, "HttpStatus must not be null");
		this.statusCode = statusCode;
	}


	/**
	 * Set the body as a UTF-8 String.
	 */
	public DefaultResponseCreator body(String content) {
		this.content = content.getBytes(UTF8_CHARSET);
		return this;
	}

	/**
	 * Set the body as a byte array.
	 */
	public DefaultResponseCreator body(byte[] content) {
		this.content = content;
		return this;
	}

	/**
	 * Set the body as a {@link Resource}.
	 */
	public DefaultResponseCreator body(Resource resource) {
		this.contentResource = resource;
		return this;
	}

	/**
	 * Set the {@code Content-Type} header.
	 */
	public DefaultResponseCreator contentType(MediaType mediaType) {
		if (mediaType != null) {
			this.headers.setContentType(mediaType);
		}
		return this;
	}

	/**
	 * Set the {@code Location} header.
	 */
	public DefaultResponseCreator location(URI location) {
		this.headers.setLocation(location);
		return this;
	}

	/**
	 * Copy all given headers.
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
