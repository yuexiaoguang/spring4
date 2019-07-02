package org.springframework.test.web.client;

import java.io.IOException;
import java.util.Iterator;

import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.Assert;

/**
 * 简单的{@code RequestExpectationManager}, 按顺序匹配期望的请求, i.e. 按照预期的声明顺序.
 *
 * <p>当请求期望具有大于1的预期计数时, 仅预期第一次执行与声明的顺序匹配.
 * 此后可以在任何地方插入后续请求执行.
 */
public class SimpleRequestExpectationManager extends AbstractRequestExpectationManager {

	/** 预期的声明顺序 (计数可能 > 1) */
	private Iterator<RequestExpectation> expectationIterator;

	/** 剩余计数的跟踪预期 */
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
