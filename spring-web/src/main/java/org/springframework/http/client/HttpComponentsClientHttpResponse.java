package org.springframework.http.client;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;

import org.springframework.http.HttpHeaders;
import org.springframework.util.StreamUtils;

/**
 * {@link ClientHttpResponse}实现, 基于Apache HttpComponents HttpClient.
 *
 * <p>通过{@link HttpComponentsClientHttpRequest}创建.
 */
final class HttpComponentsClientHttpResponse extends AbstractClientHttpResponse {

	private final HttpResponse httpResponse;

	private HttpHeaders headers;


	HttpComponentsClientHttpResponse(HttpResponse httpResponse) {
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
        // 将底层连接释放回连接管理器
        try {
            try {
                // 通过消耗其剩余内容来尝试保持连接活跃
                EntityUtils.consume(this.httpResponse.getEntity());
            }
			finally {
				if (this.httpResponse instanceof Closeable) {
					((Closeable) this.httpResponse).close();
				}
            }
        }
        catch (IOException ex) {
			// Ignore exception on close...
        }
	}

}
