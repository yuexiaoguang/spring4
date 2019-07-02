package org.springframework.mock.http.client;

import java.io.IOException;
import java.net.URI;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.MockHttpOutputMessage;

/**
 * {@link ClientHttpRequest}的模拟实现.
 */
public class MockClientHttpRequest extends MockHttpOutputMessage implements ClientHttpRequest {

	private HttpMethod httpMethod;

	private URI uri;

	private ClientHttpResponse clientHttpResponse;

	private boolean executed = false;


	public MockClientHttpRequest() {
	}

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
	 * 
	 * 将{@link #isExecuted() executed}标志设置为{@code true}, 并返回已配置的{@link #setResponse(ClientHttpResponse) 响应}.
	 */
	@Override
	public final ClientHttpResponse execute() throws IOException {
		this.executed = true;
		return executeInternal();
	}

	/**
	 * 默认实现返回已配置的{@link #setResponse(ClientHttpResponse) 响应}.
	 * <p>重写此方法以执行请求, 并提供可能与配置的响应不同的响应.
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
