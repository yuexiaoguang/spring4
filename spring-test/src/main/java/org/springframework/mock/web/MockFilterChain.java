package org.springframework.mock.web;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * {@link javax.servlet.FilterChain}接口的模拟实现.
 *
 * <p>可以使用一个或多个过滤器和要调用的Servlet配置{@link MockFilterChain}.
 * 第一次调用链时, 它会调用所有过滤器和Servlet, 并保存请求和响应.
 * 除非调用{@link #reset()}, 否则后续调用会引发{@link IllegalStateException}.
 */
public class MockFilterChain implements FilterChain {

	private ServletRequest request;

	private ServletResponse response;

	private final List<Filter> filters;

	private Iterator<Filter> iterator;


	/**
	 * 注册一个无操作的{@link Filter}实现. 第一次调用保存请求和响应.
	 * 除非调用{@link #reset()}, 否则后续调用会引发{@link IllegalStateException}.
	 */
	public MockFilterChain() {
		this.filters = Collections.emptyList();
	}

	/**
	 * @param servlet 要调用的Servlet
	 */
	public MockFilterChain(Servlet servlet) {
		this.filters = initFilterList(servlet);
	}

	/**
	 * @param servlet 要在{@link FilterChain}中调用的{@link Servlet}
	 * @param filters 要在{@link FilterChain}中调用的{@link Filter}
	 */
	public MockFilterChain(Servlet servlet, Filter... filters) {
		Assert.notNull(filters, "filters cannot be null");
		Assert.noNullElements(filters, "filters cannot contain null values");
		this.filters = initFilterList(servlet, filters);
	}

	private static List<Filter> initFilterList(Servlet servlet, Filter... filters) {
		Filter[] allFilters = ObjectUtils.addObjectToArray(filters, new ServletFilterProxy(servlet));
		return Arrays.asList(allFilters);
	}

	/**
	 * 返回已调用{@link #doFilter}的请求.
	 */
	public ServletRequest getRequest() {
		return this.request;
	}

	/**
	 * 返回已调用{@link #doFilter}的响应.
	 */
	public ServletResponse getResponse() {
		return this.response;
	}

	/**
	 * 调用已注册的{@link Filter}和/或{@link Servlet}, 同时保存请求和响应.
	 */
	@Override
	public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
		Assert.notNull(request, "Request must not be null");
		Assert.notNull(response, "Response must not be null");
		Assert.state(this.request == null, "This FilterChain has already been called!");

		if (this.iterator == null) {
			this.iterator = this.filters.iterator();
		}

		if (this.iterator.hasNext()) {
			Filter nextFilter = this.iterator.next();
			nextFilter.doFilter(request, response, this);
		}

		this.request = request;
		this.response = response;
	}

	/**
	 * 重置{@link MockFilterChain}, 允许再次调用它.
	 */
	public void reset() {
		this.request = null;
		this.response = null;
		this.iterator = null;
	}


	/**
	 * 一个简单地委托给Servlet的过滤器.
	 */
	private static class ServletFilterProxy implements Filter {

		private final Servlet delegateServlet;

		private ServletFilterProxy(Servlet servlet) {
			Assert.notNull(servlet, "servlet cannot be null");
			this.delegateServlet = servlet;
		}

		@Override
		public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
				throws IOException, ServletException {

			this.delegateServlet.service(request, response);
		}

		@Override
		public void init(FilterConfig filterConfig) throws ServletException {
		}

		@Override
		public void destroy() {
		}

		@Override
		public String toString() {
			return this.delegateServlet.toString();
		}
	}

}
