package org.springframework.mock.http.client;

import java.io.IOException;
import java.net.URI;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.AsyncClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;

/**
 * An extension of {@link MockClientHttpRequest} that also implements
 * {@link AsyncClientHttpRequest} by wrapping the response in a
 * {@link SettableListenableFuture}.
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
