package org.springframework.mock.http.client;

import java.io.IOException;
import java.net.URI;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.AsyncClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;

/**
 * {@link MockClientHttpRequest}的扩展,
 * 它通过将响应包装在{@link SettableListenableFuture}中来实现{@link AsyncClientHttpRequest}.
 */
public class MockAsyncClientHttpRequest extends MockClientHttpRequest implements AsyncClientHttpRequest {

	public MockAsyncClientHttpRequest() {
	}

	public MockAsyncClientHttpRequest(HttpMethod httpMethod, URI uri) {
		super(httpMethod, uri);
	}


	@Override
	public ListenableFuture<ClientHttpResponse> executeAsync() throws IOException {
		SettableListenableFuture<ClientHttpResponse> future = new SettableListenableFuture<ClientHttpResponse>();
		future.set(execute());
		return future;
	}

}
