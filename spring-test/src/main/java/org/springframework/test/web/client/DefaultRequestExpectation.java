package org.springframework.test.web.client;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.Assert;

/**
 * Default implementation of {@code RequestExpectation} that simply delegates
 * to the request matchers and the response creator it contains.
 */
public class DefaultRequestExpectation implements RequestExpectation {

	private final RequestCount requestCount;

	private final List<RequestMatcher> requestMatchers = new LinkedList<RequestMatcher>();

	private ResponseCreator responseCreator;


	/**
	 * Create a new request expectation that should be called a number of times
	 * as indicated by {@code RequestCount}.
	 * @param expectedCount the expected request expectedCount
	 */
	public DefaultRequestExpectation(ExpectedCount expectedCount, RequestMatcher requestMatcher) {
		Assert.notNull(expectedCount, "ExpectedCount is required");
		Assert.notNull(requestMatcher, "RequestMatcher is required");
		this.requestCount = new RequestCount(expectedCount);
		this.requestMatchers.add(requestMatcher);
	}


	protected RequestCount getRequestCount() {
		return this.requestCount;
	}

	protected List<RequestMatcher> getRequestMatchers() {
		return this.requestMatchers;
	}

	protected ResponseCreator getResponseCreator() {
		return this.responseCreator;
	}

	@Override
	public ResponseActions andExpect(RequestMatcher requestMatcher) {
		Assert.notNull(requestMatcher, "RequestMatcher is required");
		this.requestMatchers.add(requestMatcher);
		return this;
	}

	@Override
	public void andRespond(ResponseCreator responseCreator) {
		Assert.notNull(responseCreator, "ResponseCreator is required");
		this.responseCreator = responseCreator;
	}

	@Override
	public void match(ClientHttpRequest request) throws IOException {
		for (RequestMatcher matcher : getRequestMatchers()) {
			matcher.match(request);
		}
	}

	@Override
	public ClientHttpResponse createResponse(ClientHttpRequest request) throws IOException {
		ResponseCreator responseCreator = getResponseCreator();
		if (responseCreator == null) {
			throw new IllegalStateException("createResponse called before ResponseCreator was set");
		}
		getRequestCount().incrementAndValidate();
		return responseCreator.createResponse(request);
	}

	@Override
	public boolean hasRemainingCount() {
		return getRequestCount().hasRemainingCount();
	}

	@Override
	public boolean isSatisfied() {
		return getRequestCount().isSatisfied();
	}


	/**
	 * Helper class that keeps track of actual vs expected request count.
	 */
	protected static class RequestCount {

		private final ExpectedCount expectedCount;

		private int matchedRequestCount;

		public RequestCount(ExpectedCount expectedCount) {
			this.expectedCount = expectedCount;
		}

		public ExpectedCount getExpectedCount() {
			return this.expectedCount;
		}

		public int getMatchedRequestCount() {
			return this.matchedRequestCount;
		}

		public void incrementAndValidate() {
			this.matchedRequestCount++;
			if (getMatchedRequestCount() > getExpectedCount().getMaxCount()) {
				throw new AssertionError("No more calls expected.");
			}
		}

		public boolean hasRemainingCount() {
			return (getMatchedRequestCount() < getExpectedCount().getMaxCount());
		}

		public boolean isSatisfied() {
			// Only validate min count since max count is checked on every request...
			return (getMatchedRequestCount() >= getExpectedCount().getMinCount());
		}
	}

}
