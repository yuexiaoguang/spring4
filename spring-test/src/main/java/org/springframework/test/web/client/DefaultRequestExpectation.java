package org.springframework.test.web.client;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.Assert;

/**
 * {@code RequestExpectation}的默认实现, 它只是委托给请求匹配器及其包含的响应创建者.
 */
public class DefaultRequestExpectation implements RequestExpectation {

	private final RequestCount requestCount;

	private final List<RequestMatcher> requestMatchers = new LinkedList<RequestMatcher>();

	private ResponseCreator responseCreator;


	/**
	 * @param expectedCount 预期请求expectedCount次数
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
	 * Helper类, 用于跟踪实际与预期的请求计数.
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
			// 仅验证最小计数, 因为每次请求都会检查最大计数...
			return (getMatchedRequestCount() >= getExpectedCount().getMinCount());
		}
	}

}
