package org.springframework.http.client;

import java.io.IOException;
import java.net.URI;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

/**
 * {@link ClientHttpRequest} implementation based on OkHttp 2.x.
 *
 * <p>Created via the {@link OkHttpClientHttpRequestFactory}.
 */
class OkHttpClientHttpRequest extends AbstractBufferingClientHttpRequest {

	private final OkHttpClient client;

	private final URI uri;

	private final HttpMethod method;


	public OkHttpClientHttpRequest(OkHttpClient client, URI uri, HttpMethod method) {
		this.client = client;
		this.uri = uri;
		this.method = method;
	}


	@Override
	public HttpMethod getMethod() {
		return this.method;
	}

	@Override
	public URI getURI() {
		return this.uri;
	}


	@Override
	protected ClientHttpResponse executeInternal(HttpHeaders headers, byte[] content) throws IOException {
		Request request = OkHttpClientHttpRequestFactory.buildRequest(headers, content, this.uri, this.method);
		return new OkHttpClientHttpResponse(this.client.newCall(request).execute());
	}

}
