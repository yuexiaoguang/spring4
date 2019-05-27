package org.springframework.http.client;

import java.io.IOException;
import java.net.URI;

import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;

/**
 * {@link AsyncClientHttpRequest} implementation based on OkHttp 2.x.
 *
 * <p>Created via the {@link OkHttpClientHttpRequestFactory}.
 */
class OkHttpAsyncClientHttpRequest extends AbstractBufferingAsyncClientHttpRequest {

	private final OkHttpClient client;

	private final URI uri;

	private final HttpMethod method;


	public OkHttpAsyncClientHttpRequest(OkHttpClient client, URI uri, HttpMethod method) {
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

		Request request = OkHttpClientHttpRequestFactory.buildRequest(headers, content, this.uri, this.method);
		return new OkHttpListenableFuture(this.client.newCall(request));
	}


	private static class OkHttpListenableFuture extends SettableListenableFuture<ClientHttpResponse> {

		private final Call call;

		public OkHttpListenableFuture(Call call) {
			this.call = call;
			this.call.enqueue(new Callback() {
				@Override
				public void onResponse(Response response) {
					set(new OkHttpClientHttpResponse(response));
				}
				@Override
				public void onFailure(Request request, IOException ex) {
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
