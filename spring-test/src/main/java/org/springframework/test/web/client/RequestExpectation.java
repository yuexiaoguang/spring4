package org.springframework.test.web.client;

/**
 * An extension of {@code ResponseActions} that also implements
 * {@code RequestMatcher} and {@code ResponseCreator}
 *
 * <p>While {@code ResponseActions} is the API for defining expectations this
 * sub-interface is the internal SPI for matching these expectations to actual
 * requests and for creating responses.
 */
public interface RequestExpectation extends ResponseActions, RequestMatcher, ResponseCreator {

	/**
	 * Whether there is a remaining count of invocations for this expectation.
	 */
	boolean hasRemainingCount();

	/**
	 * Whether the requirements for this request expectation have been met.
	 */
	boolean isSatisfied();

}
