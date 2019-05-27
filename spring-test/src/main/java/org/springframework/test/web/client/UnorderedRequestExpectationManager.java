package org.springframework.test.web.client;

import java.io.IOException;

import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;

/**
 * {@code RequestExpectationManager} that matches requests to expectations
 * regardless of the order of declaration of expected requests.
 */
public class UnorderedRequestExpectationManager extends AbstractRequestExpectationManager {

	private final RequestExpectationGroup remainingExpectations = new RequestExpectationGroup();


	@Override
	protected void afterExpectationsDeclared() {
		this.remainingExpectations.updateAll(getExpectations());
	}

	@Override
	public ClientHttpResponse validateRequestInternal(ClientHttpRequest request) throws IOException {
		RequestExpectation expectation = this.remainingExpectations.findExpectation(request);
		if (expectation != null) {
			ClientHttpResponse response = expectation.createResponse(request);
			this.remainingExpectations.update(expectation);
			return response;
		}
		throw createUnexpectedRequestError(request);
	}

	@Override
	public void reset() {
		super.reset();
		this.remainingExpectations.reset();
	}

}
