package org.springframework.test.web.servlet.result;

import java.util.concurrent.Callable;
import javax.servlet.http.HttpServletRequest;

import org.hamcrest.Matcher;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.context.request.async.WebAsyncTask;

import static org.hamcrest.MatcherAssert.*;
import static org.springframework.test.util.AssertionErrors.*;

/**
 * Factory for assertions on the request.
 *
 * <p>An instance of this class is typically accessed via
 * {@link MockMvcResultMatchers#request}.
 */
public class RequestResultMatchers {

	/**
	 * Protected constructor.
	 * <p>Use {@link MockMvcResultMatchers#request()}.
	 */
	protected RequestResultMatchers() {
	}


	/**
	 * Assert whether asynchronous processing started, usually as a result of a
	 * controller method returning {@link Callable} or {@link DeferredResult}.
	 * <p>The test will await the completion of a {@code Callable} so that
	 * {@link #asyncResult(Matcher)} can be used to assert the resulting value.
	 * Neither a {@code Callable} nor a {@code DeferredResult} will complete
	 * processing all the way since a {@link MockHttpServletRequest} does not
	 * perform asynchronous dispatches.
	 */
	public ResultMatcher asyncStarted() {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) {
				HttpServletRequest request = result.getRequest();
				assertAsyncStarted(request);
			}
		};
	}

	/**
	 * Assert that asynchronous processing was not started.
	 * @see #asyncStarted()
	 */
	public ResultMatcher asyncNotStarted() {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) {
				HttpServletRequest request = result.getRequest();
				assertEquals("Async started", false, request.isAsyncStarted());
			}
		};
	}

	/**
	 * Assert the result from asynchronous processing with the given matcher.
	 * <p>This method can be used when a controller method returns {@link Callable}
	 * or {@link WebAsyncTask}.
	 */
	public <T> ResultMatcher asyncResult(final Matcher<T> matcher) {
		return new ResultMatcher() {
			@Override
			@SuppressWarnings("unchecked")
			public void match(MvcResult result) {
				HttpServletRequest request = result.getRequest();
				assertAsyncStarted(request);
				assertThat("Async result", (T) result.getAsyncResult(), matcher);
			}
		};
	}

	/**
	 * Assert the result from asynchronous processing.
	 * <p>This method can be used when a controller method returns {@link Callable}
	 * or {@link WebAsyncTask}. The value matched is the value returned from the
	 * {@code Callable} or the exception raised.
	 */
	public <T> ResultMatcher asyncResult(final Object expectedResult) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) {
				HttpServletRequest request = result.getRequest();
				assertAsyncStarted(request);
				assertEquals("Async result", expectedResult, result.getAsyncResult());
			}
		};
	}

	/**
	 * Assert a request attribute value with the given Hamcrest {@link Matcher}.
	 */
	public <T> ResultMatcher attribute(final String name, final Matcher<T> matcher) {
		return new ResultMatcher() {
			@Override
			@SuppressWarnings("unchecked")
			public void match(MvcResult result) {
				T value = (T) result.getRequest().getAttribute(name);
				assertThat("Request attribute '" + name + "'", value, matcher);
			}
		};
	}

	/**
	 * Assert a request attribute value.
	 */
	public <T> ResultMatcher attribute(final String name, final Object expectedValue) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) {
				assertEquals("Request attribute '" + name + "'", expectedValue, result.getRequest().getAttribute(name));
			}
		};
	}

	/**
	 * Assert a session attribute value with the given Hamcrest {@link Matcher}.
	 */
	public <T> ResultMatcher sessionAttribute(final String name, final Matcher<T> matcher) {
		return new ResultMatcher() {
			@Override
			@SuppressWarnings("unchecked")
			public void match(MvcResult result) {
				T value = (T) result.getRequest().getSession().getAttribute(name);
				assertThat("Session attribute '" + name + "'", value, matcher);
			}
		};
	}

	/**
	 * Assert a session attribute value.
	 */
	public <T> ResultMatcher sessionAttribute(final String name, final Object value) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) {
				assertEquals("Session attribute '" + name + "'", value, result.getRequest().getSession().getAttribute(name));
			}
		};
	}

	private static void assertAsyncStarted(HttpServletRequest request) {
		assertEquals("Async started", true, request.isAsyncStarted());
	}

}
