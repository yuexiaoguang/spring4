package org.springframework.http.client;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.util.StreamUtils;

/**
 * {@link ClientHttpResponse} implementation based on
 * Apache HttpComponents HttpAsyncClient.
 *
 * <p>Created via the {@link HttpComponentsAsyncClientHttpRequest}.
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
        // HTTP responses returned by async HTTP client are not bound to an
        // active connection and do not have to deallocate any resources...
	}

}
