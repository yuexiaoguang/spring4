package org.springframework.web.client;

import java.io.IOException;

import org.springframework.http.client.AsyncClientHttpRequest;

/**
 * Callback interface for code that operates on an {@link AsyncClientHttpRequest}. Allows
 * to manipulate the request headers, and write to the request body.
 *
 * <p>Used internally by the {@link AsyncRestTemplate}, but also useful for application code.
 */
public interface AsyncRequestCallback {

	/**
	 * Gets called by {@link AsyncRestTemplate#execute} with an opened {@code ClientHttpRequest}.
	 * Does not need to care about closing the request or about handling errors:
	 * this will all be handled by the {@code RestTemplate}.
	 * @param request the active HTTP request
	 * @throws java.io.IOException in case of I/O errors
	 */
	void doWithRequest(AsyncClientHttpRequest request) throws IOException;

}
