package org.springframework.http.client;

import java.io.IOException;
import java.net.URI;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;

/**
 * 基于OkHttp 3.x的{@link AsyncClientHttpRequest}实现.
 *
 * <p>通过{@link OkHttp3ClientHttpRequestFactory}创建.
 */
class OkHttp3AsyncClientHttpRequest extends AbstractBufferingAsyncClientHttpRequest {

	private final OkHttpClient client;

	private final URI uri;

	private final HttpMethod method;


	public OkHttp3AsyncClientHttpRequest(OkHttpClient client, URI uri, HttpMethod method) {
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
	protected ListenableFuture<ClientHttpResponse> executeInternal(HttpHeaders headers, byte[] content)
			throws IOException {

		Request request = OkHttp3ClientHttpRequestFactory.buildRequest(headers, content, this.uri, this.method);
		return new OkHttpListenableFuture(this.client.newCall(request));
	}


	private static class OkHttpListenableFuture extends SettableListenableFuture<ClientHttpResponse> {

		private final Call call;

		public OkHttpListenableFuture(Call call) {
			this.call = call;
			this.call.enqueue(new Callback() {
				@Override
				public void onResponse(Call call, Response response) {
					set(new OkHttp3ClientHttpResponse(response));
				}
				@Override
				public void onFailure(Call call, IOException ex) {
					setException(ex);
				}
			});
		}

		@Override
		protected void interruptTask() {
			this.call.cancel();
		}
	}

}
