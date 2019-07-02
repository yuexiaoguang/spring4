package org.springframework.mock.web;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.springframework.util.Assert;

/**
 * {@link javax.servlet.FilterConfig}接口的实现, 它简单地将调用传递给给定的Filter/FilterChain组合
 * (指示链中的下一个Filter以及它应该处理的FilterChain)或给定的Servlet (表示链的末尾).
 */
public class PassThroughFilterChain implements FilterChain {

	private Filter filter;

	private FilterChain nextFilterChain;

	private Servlet servlet;


	/**
	 * @param filter 要委托给的Filter
	 * @param nextFilterChain 用于下一个Filter的FilterChain
	 */
	public PassThroughFilterChain(Filter filter, FilterChain nextFilterChain) {
		Assert.notNull(filter, "Filter must not be null");
		Assert.notNull(nextFilterChain, "'FilterChain must not be null");
		this.filter = filter;
		this.nextFilterChain = nextFilterChain;
	}

	/**
	 * @param servlet 要委托给的Servlet
	 */
	public PassThroughFilterChain(Servlet servlet) {
		Assert.notNull(servlet, "Servlet must not be null");
		this.servlet = servlet;
	}


	/**
	 * 将调用传递给Filter/Servlet.
	 */
	@Override
	public void doFilter(ServletRequest request, ServletResponse response) throws ServletException, IOException {
		if (this.filter != null) {
			this.filter.doFilter(request, response, this.nextFilterChain);
		}
		else {
			this.servlet.service(request, response);
		}
	}

}
