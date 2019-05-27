package org.springframework.test.web.client;

import java.io.IOException;

import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.web.client.response.MockRestResponseCreators;

/**
 * A contract for creating a {@link ClientHttpResponse}.
 * Implementations can be obtained via {@link MockRestResponseCreators}.
 */
public interface ResponseCreator {

	/**
	 * Create a response for the given request.
	 * @param request the request
	 */
	ClientHttpResponse createResponse(ClientHttpRequest request) throws IOException;

}
