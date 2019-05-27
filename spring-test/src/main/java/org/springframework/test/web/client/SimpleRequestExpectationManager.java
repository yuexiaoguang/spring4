package org.springframework.test.web.client;

import java.io.IOException;
import java.util.Iterator;

import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.Assert;

/**
 * Simple {@code RequestExpectationManager} that matches requests to expectations
 * sequentially, i.e. in the order of declaration of expectations.
 *
 * <p>When request expectations have an expected count greater than one,
 * only the first execution is expected to match the order of declaration.
 * Subsequent request executions may be inserted anywhere thereafter.
 */
public class SimpleRequestExpectationManager extends AbstractRequestExpectationManager {

	/** Expectations in the order of declaration (count may be > 1) */
	private Iterator<RequestExpectation> expectationIterator;

	/** Track expectations that have a remaining count */
	private final RequestExpectationGroup repeatExpectations = new RequestExpectationGroup();


	@Override
	protected void afterExpectationsDeclared() {
		Assert.state(this.expectationIterator == null, "Expectations already declared");
		this.expectationIterator = getExpectations().iterator();
	}

	@Override
	public ClientHttpResponse validateRequestInternal(ClientHttpRequest request) throws IOException {
		RequestExpectation expectation = this.repeatExpectations.findExpectation(request);
		if (expectation == null) {
			if (!this.expectationIterator.hasNext()) {
				throw createUnexpectedRequestError(request);
			}
			expectation = this.expectationIterator.next();
			expectation.match(request);
		}
		ClientHttpResponse response = expectation.createResponse(request);
		this.repeatExpectations.update(expectation);
		return response;
	}

	@Override
	public void reset() {
		super.reset();
		this.expectationIterator = null;
		this.repeatExpectations.reset();
	}

}
