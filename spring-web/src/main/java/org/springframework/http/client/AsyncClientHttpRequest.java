package org.springframework.http.client;

import java.io.IOException;

import org.springframework.http.HttpOutputMessage;
import org.springframework.http.HttpRequest;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * Represents a client-side asynchronous HTTP request. Created via an
 * implementation of the {@link AsyncClientHttpRequestFactory}.
 *
 * <p>A {@code AsyncHttpRequest} can be {@linkplain #executeAsync() executed},
 * getting a future {@link ClientHttpResponse} which can be read from.
 */
public interface AsyncClientHttpRequest extends HttpRequest, HttpOutputMessage {

	/**
	 * Execute this request asynchronously, resulting in a Future handle.
	 * {@link ClientHttpResponse} that can be read.
	 * @return the future response result of the execution
	 * @throws java.io.IOException in case of I/O errors
	 */
	ListenableFuture<ClientHttpResponse> executeAsync() throws IOException;

}
