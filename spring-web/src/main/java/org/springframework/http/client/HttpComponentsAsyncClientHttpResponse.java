package org.springframework.http.client;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.util.StreamUtils;

/**
 * {@link ClientHttpResponse}实现, 基于Apache HttpComponents HttpAsyncClient.
 *
 * <p>通过{@link HttpComponentsAsyncClientHttpRequest}创建.
 */
final class HttpComponentsAsyncClientHttpResponse extends AbstractClientHttpResponse {

	private final HttpResponse httpResponse;

	private HttpHeaders headers;


	HttpComponentsAsyncClientHttpResponse(HttpResponse httpResponse) {
		this.httpResponse = httpResponse;
	}


	@Override
	public int getRawStatusCode() throws IOException {
		return this.httpResponse.getStatusLine().getStatusCode();
	}

	@Override
	public String getStatusText() throws IOException {
		return this.httpResponse.getStatusLine().getReasonPhrase();
	}

	@Override
	public HttpHeaders getHeaders() {
		if (this.headers == null) {
			this.headers = new HttpHeaders();
			for (Header header : this.httpResponse.getAllHeaders()) {
				this.headers.add(header.getName(), header.getValue());
			}
		}
		return this.headers;
	}

	@Override
	public InputStream getBody() throws IOException {
		HttpEntity entity = this.httpResponse.getEntity();
		return (entity != null ? entity.getContent() : StreamUtils.emptyInput());
	}

	@Override
	public void close() {
        // 异步HTTP客户端返回的HTTP响应未绑定到活动连接, 并且不必释放任何资源...
	}

}
