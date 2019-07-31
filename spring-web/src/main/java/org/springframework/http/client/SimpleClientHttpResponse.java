package org.springframework.http.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

import org.springframework.http.HttpHeaders;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

/**
 * {@link ClientHttpResponse}实现, 使用标准JDK工具.
 * 通过{@link SimpleBufferingClientHttpRequest#execute()}和{@link SimpleStreamingClientHttpRequest#execute()}获取.
 */
final class SimpleClientHttpResponse extends AbstractClientHttpResponse {

	private final HttpURLConnection connection;

	private HttpHeaders headers;

	private InputStream responseStream;


	SimpleClientHttpResponse(HttpURLConnection connection) {
		this.connection = connection;
	}


	@Override
	public int getRawStatusCode() throws IOException {
		return this.connection.getResponseCode();
	}

	@Override
	public String getStatusText() throws IOException {
		return this.connection.getResponseMessage();
	}

	@Override
	public HttpHeaders getHeaders() {
		if (this.headers == null) {
			this.headers = new HttpHeaders();
			// Header 字段 0 是大多数HttpURLConnections的状态行, 但不是GAE的状态行
			String name = this.connection.getHeaderFieldKey(0);
			if (StringUtils.hasLength(name)) {
				this.headers.add(name, this.connection.getHeaderField(0));
			}
			int i = 1;
			while (true) {
				name = this.connection.getHeaderFieldKey(i);
				if (!StringUtils.hasLength(name)) {
					break;
				}
				this.headers.add(name, this.connection.getHeaderField(i));
				i++;
			}
		}
		return this.headers;
	}

	@Override
	public InputStream getBody() throws IOException {
		InputStream errorStream = this.connection.getErrorStream();
		this.responseStream = (errorStream != null ? errorStream : this.connection.getInputStream());
		return this.responseStream;
	}

	@Override
	public void close() {
		if (this.responseStream != null) {
			try {
				StreamUtils.drain(this.responseStream);
				this.responseStream.close();
			}
			catch (Exception ex) {
				// ignore
			}
		}
	}

}
