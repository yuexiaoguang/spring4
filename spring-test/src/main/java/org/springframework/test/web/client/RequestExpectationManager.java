package org.springframework.test.web.client;

import java.io.IOException;

import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;

/**
 * Encapsulates the behavior required to implement {@link MockRestServiceServer}
 * including its public API (create expectations + verify/reset) along with an
 * extra method for verifying actual requests.
 *
 * <p>This contract is not used directly in applications but a custom
 * implementation can be
 * {@link org.springframework.test.web.client.MockRestServiceServer.MockRestServiceServerBuilder#build(RequestExpectationManager)
 * plugged} in through the {@code MockRestServiceServer} builder.
 */
public interface RequestExpectationManager {

	/**
	 * Set up a new request expectation. The returned {@link ResponseActions} is
	 * used to add more expectations and define a response.
	 * <p>This is a delegate for
	 * {@link MockRestServiceServer#expect(ExpectedCount, RequestMatcher)}.
	 *
	 * @param requestMatcher a request expectation
	 * @return for setting up further expectations and define a response
	 */
	ResponseActions expectRequest(ExpectedCount count, RequestMatcher requestMatcher);

	/**
	 * Verify that all expectations have been met.
	 * <p>This is a delegate for {@link MockRestServiceServer#verify()}.
	 * @throws AssertionError when some expectations were not met
	 * @see MockRestServiceServer#verify()
	 */
	void verify();

	/**
	 * Reset the internal state removing all expectations and recorded requests.
	 * <p>This is a delegate for {@link MockRestServiceServer#reset()}.
	 * @see MockRestServiceServer#reset()
	 */
	void reset();


	/**
	 * Validate the given actual request against the declared expectations.
	 * Is successful return the mock response to use or raise an error.
	 * <p>This is used in {@link MockRestServiceServer} against actual requests.
	 * @param request the request
	 * @return the response to return if the request was validated.
	 * @throws AssertionError when some expectations were not met
	 * @throws IOException in case of any validation errors
	 */
	ClientHttpResponse validateRequest(ClientHttpRequest request) throws IOException;

}
