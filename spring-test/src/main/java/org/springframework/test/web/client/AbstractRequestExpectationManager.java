package org.springframework.test.web.client;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.Assert;

/**
 * {@code RequestExpectationManager}实现的基类, 负责存储期望和实际请求, 并在最后检查未满足的期望.
 *
 * <p>子类负责通过将每个请求与遵循声明顺序的期望匹配, 来验证每个请求.
 */
public abstract class AbstractRequestExpectationManager implements RequestExpectationManager {

	private final List<RequestExpectation> expectations = new LinkedList<RequestExpectation>();

	private final List<ClientHttpRequest> requests = new LinkedList<ClientHttpRequest>();


	protected List<RequestExpectation> getExpectations() {
		return this.expectations;
	}

	protected List<ClientHttpRequest> getRequests() {
		return this.requests;
	}


	@Override
	public ResponseActions expectRequest(ExpectedCount count, RequestMatcher matcher) {
		Assert.state(getRequests().isEmpty(), "Cannot add more expectations after actual requests are made");
		RequestExpectation expectation = new DefaultRequestExpectation(count, matcher);
		getExpectations().add(expectation);
		return expectation;
	}

	@Override
	public ClientHttpResponse validateRequest(ClientHttpRequest request) throws IOException {
		List<ClientHttpRequest> requests = getRequests();
		synchronized (requests) {
			if (requests.isEmpty()) {
				afterExpectationsDeclared();
			}
			try {
				return validateRequestInternal(request);
			}
			finally {
				requests.add(request);
			}
		}
	}

	/**
	 * 在第一次实际请求时调用, 这实际上意味着期望声明阶段结束.
	 */
	protected void afterExpectationsDeclared() {
	}

	/**
	 * 子类必须实现与声明的期望匹配的请求的实际验证.
	 */
	protected abstract ClientHttpResponse validateRequestInternal(ClientHttpRequest request)
			throws IOException;

	@Override
	public void verify() {
		if (getExpectations().isEmpty()) {
			return;
		}
		int count = 0;
		for (RequestExpectation expectation : getExpectations()) {
			if (!expectation.isSatisfied()) {
				count++;
			}
		}
		if (count > 0) {
			String message = "Further request(s) expected leaving " + count + " unsatisfied expectation(s).\n";
			throw new AssertionError(message + getRequestDetails());
		}
	}

	/**
	 * 返回执行请求的详细信息.
	 */
	protected String getRequestDetails() {
		StringBuilder sb = new StringBuilder();
		sb.append(getRequests().size()).append(" request(s) executed");
		if (!getRequests().isEmpty()) {
			sb.append(":\n");
			for (ClientHttpRequest request : getRequests()) {
				sb.append(request.toString()).append("\n");
			}
		}
		else {
			sb.append(".\n");
		}
		return sb.toString();
	}

	/**
	 * 返回一个子类可以为意外请求引发的{@code AssertionError}.
	 */
	protected AssertionError createUnexpectedRequestError(ClientHttpRequest request) {
		HttpMethod method = request.getMethod();
		URI uri = request.getURI();
		String message = "No further requests expected: HTTP " + method + " " + uri + "\n";
		return new AssertionError(message + getRequestDetails());
	}

	@Override
	public void reset() {
		this.expectations.clear();
		this.requests.clear();
	}


	/**
	 * 管理一组剩余的期望的助手类.
	 */
	protected static class RequestExpectationGroup {

		private final Set<RequestExpectation> expectations = new LinkedHashSet<RequestExpectation>();

		public Set<RequestExpectation> getExpectations() {
			return this.expectations;
		}

		/**
		 * 返回匹配的期望, 如果没有匹配则返回{@code null}.
		 */
		public RequestExpectation findExpectation(ClientHttpRequest request) throws IOException {
			for (RequestExpectation expectation : getExpectations()) {
				try {
					expectation.match(request);
					return expectation;
				}
				catch (AssertionError error) {
					// 正在寻找匹配或返回null..
				}
			}
			return null;
		}

		/**
		 * 调用此项以获得匹配的期望.
		 * <p>如果给定的期望具有剩余计数, 则将被存储, 否则将被删除.
		 */
		public void update(RequestExpectation expectation) {
			if (expectation.hasRemainingCount()) {
				getExpectations().add(expectation);
			}
			else {
				getExpectations().remove(expectation);
			}
		}

		/**
		 * {@link #update(RequestExpectation)}的集合变体, 可用于插入期望.
		 */
		public void updateAll(Collection<RequestExpectation> expectations) {
			for (RequestExpectation expectation : expectations) {
				update(expectation);
			}
		}

		/**
		 * 重置该组的所有期望.
		 */
		public void reset() {
			getExpectations().clear();
		}
	}
}
