package org.springframework.http.client;

import java.io.IOException;
import java.net.URI;

import org.springframework.http.HttpMethod;

/**
 * Factory for {@link AsyncClientHttpRequest} objects.
 * Requests are created by the {@link #createAsyncRequest(URI, HttpMethod)} method.
 */
public interface AsyncClientHttpRequestFactory {

	/**
	 * Create a new asynchronous {@link AsyncClientHttpRequest} for the specified URI
	 * and HTTP method.
	 * <p>The returned request can be written to, and then executed by calling
	 * {@link AsyncClientHttpRequest#executeAsync()}.
	 * @param uri the URI to create a request for
	 * @param httpMethod the HTTP method to execute
	 * @return the created request
	 * @throws IOException in case of I/O errors
	 */
	AsyncClientHttpRequest createAsyncRequest(URI uri, HttpMethod httpMethod) throws IOException;

}
