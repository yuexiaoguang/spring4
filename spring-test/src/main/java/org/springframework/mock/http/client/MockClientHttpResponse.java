package org.springframework.mock.http.client;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.util.Assert;

/**
 * {@link ClientHttpResponse}的模拟实现.
 */
public class MockClientHttpResponse extends MockHttpInputMessage implements ClientHttpResponse {

	private final HttpStatus status;


	public MockClientHttpResponse(byte[] body, HttpStatus statusCode) {
		super(body);
		Assert.notNull(statusCode, "HttpStatus is required");
		this.status = statusCode;
	}

	public MockClientHttpResponse(InputStream body, HttpStatus statusCode) {
		super(body);
		Assert.notNull(statusCode, "HttpStatus is required");
		this.status = statusCode;
	}


	@Override
	public HttpStatus getStatusCode() throws IOException {
		return this.status;
	}

	@Override
	public int getRawStatusCode() throws IOException {
		return this.status.value();
	}

	@Override
	public String getStatusText() throws IOException {
		return this.status.getReasonPhrase();
	}

	@Override
	public void close() {
		try {
			getBody().close();
		}
		catch (IOException ex) {
			// ignore
		}
	}

}
