package org.springframework.http.client;

import java.io.IOException;
import java.net.URI;

import org.springframework.http.HttpMethod;

/**
 * Wrapper for a {@link ClientHttpRequestFactory} that buffers
 * all outgoing and incoming streams in memory.
 *
 * <p>Using this wrapper allows for multiple reads of the
 * @linkplain ClientHttpResponse#getBody() response body}.
 */
public class BufferingClientHttpRequestFactory extends AbstractClientHttpRequestFactoryWrapper {

	/**
	 * Create a buffering wrapper for the given {@link ClientHttpRequestFactory}.
	 * @param requestFactory the target request factory to wrap
	 */
	public BufferingClientHttpRequestFactory(ClientHttpRequestFactory requestFactory) {
		super(requestFactory);
	}


	@Override
	protected ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod, ClientHttpRequestFactory requestFactory)
			throws IOException {

		ClientHttpRequest request = requestFactory.createRequest(uri, httpMethod);
		if (shouldBuffer(uri, httpMethod)) {
			return new BufferingClientHttpRequestWrapper(request);
		}
		else {
			return request;
		}
	}

	/**
	 * Indicates whether the request/response exchange for the given URI and method
	 * should be buffered in memory.
	 * <p>The default implementation returns {@code true} for all URIs and methods.
	 * Subclasses can override this method to change this behavior.
	 * @param uri the URI
	 * @param httpMethod the method
	 * @return {@code true} if the exchange should be buffered; {@code false} otherwise
	 */
	protected boolean shouldBuffer(URI uri, HttpMethod httpMethod) {
		return true;
	}

}
