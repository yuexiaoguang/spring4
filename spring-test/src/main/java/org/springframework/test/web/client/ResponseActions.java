package org.springframework.test.web.client;

/**
 * A contract for setting up request expectations and defining a response.
 * Implementations can be obtained through {@link MockRestServiceServer#expect(RequestMatcher)}.
 */
public interface ResponseActions {

	/**
	 * Add a request expectation.
	 * @return the expectation
	 */
	ResponseActions andExpect(RequestMatcher requestMatcher);

	/**
	 * Define the response.
	 * @param responseCreator the creator of the response
	 */
	void andRespond(ResponseCreator responseCreator);

}
