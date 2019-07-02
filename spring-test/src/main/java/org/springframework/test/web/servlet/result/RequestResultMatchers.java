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
 * 在请求时断言的工厂.
 *
 * <p>通常通过{@link MockMvcResultMatchers#request}访问此类的实例.
 */
public class RequestResultMatchers {

	/**
	 * <p>Use {@link MockMvcResultMatchers#request()}.
	 */
	protected RequestResultMatchers() {
	}


	/**
	 * 断言异步处理是否开始, 通常是由于控制器方法返回{@link Callable}或{@link DeferredResult}.
	 * <p>测试将等待{@code Callable}的完成, 以便{@link #asyncResult(Matcher)}可用于断言结果值.
	 * {@code Callable}和 {@code DeferredResult}都不会完全处理, 因为{@link MockHttpServletRequest}不执行异步调度.
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
	 * 断言异步处理未启动.
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
	 * 使用给定匹配器的异步处理断言结果.
	 * <p>当控制器方法返回{@link Callable}或{@link WebAsyncTask}时, 可以使用此方法.
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
	 * 断言异步处理的结果.
	 * <p>当控制器方法返回{@link Callable}或{@link WebAsyncTask}时, 可以使用此方法.
	 * 匹配的值是从{@code Callable}返回的值或引发的异常.
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
	 * 使用给定的Hamcrest {@link Matcher}断言请求属性值.
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
	 * 断言请求属性值.
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
	 * 使用给定的Hamcrest {@link Matcher}断言会话属性值.
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
	 * 断言会话属性值.
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
