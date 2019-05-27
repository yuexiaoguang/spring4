package org.springframework.mock.http.client;

import java.io.IOException;
import java.net.URI;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.MockHttpOutputMessage;

/**
 * Mock implementation of {@link ClientHttpRequest}.
 */
public class MockClientHttpRequest extends MockHttpOutputMessage implements ClientHttpRequest {

	private HttpMethod httpMethod;

	private URI uri;

	private ClientHttpResponse clientHttpResponse;

	private boolean executed = false;


	/**
	 * Default constructor.
	 */
	public MockClientHttpRequest() {
	}

	/**
	 * Create an instance with the given HttpMethod and URI.
	 */
	public MockClientHttpRequest(HttpMethod httpMethod, URI uri) {
		this.httpMethod = httpMethod;
		this.uri = uri;
	}


	public void setMethod(HttpMethod httpMethod) {
		this.httpMethod = httpMethod;
	}

	@Override
	public HttpMethod getMethod() {
		return this.httpMethod;
	}

	public void setURI(URI uri) {
		this.uri = uri;
	}

	@Override
	public URI getURI() {
		return this.uri;
	}

	public void setResponse(ClientHttpResponse clientHttpResponse) {
		this.clientHttpResponse = clientHttpResponse;
	}

	public boolean isExecuted() {
		return this.executed;
	}

	/**
	 * Set the {@link #isExecuted() executed} flag to {@code true} and return the
	 * configured {@link #setResponse(ClientHttpResponse) response}.
	 * @see #executeInternal()
	 */
	@Override
	public final ClientHttpResponse execute() throws IOException {
		this.executed = true;
		return executeInternal();
	}

	/**
	 * The default implementation returns the configured
	 * {@link #setResponse(ClientHttpResponse) response}.
	 * <p>Override this method to execute the request and provide a response,
	 * potentially different than the configured response.
	 */
	protected ClientHttpResponse executeInternal() throws IOException {
		return this.clientHttpResponse;
	}


	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (this.httpMethod != null) {
			sb.append(this.httpMethod);
		}
		if (this.uri != null) {
			sb.append(" ").append(this.uri);
		}
		if (!getHeaders().isEmpty()) {
			sb.append(", headers: ").append(getHeaders());
		}
		if (sb.length() == 0) {
			sb.append("Not yet initialized");
		}
		return sb.toString();
	}

}
