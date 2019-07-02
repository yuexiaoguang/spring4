package org.springframework.mock.web;

import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;

import org.springframework.util.Assert;

/**
 * {@link javax.servlet.FilterConfig}接口的模拟实现.
 *
 * <p>用于测试Web框架; 对于测试自定义{@link javax.servlet.Filter}实现也很有用.
 */
public class MockFilterConfig implements FilterConfig {

	private final ServletContext servletContext;

	private final String filterName;

	private final Map<String, String> initParameters = new LinkedHashMap<String, String>();


	/**
	 * 使用默认的{@link MockServletContext}.
	 */
	public MockFilterConfig() {
		this(null, "");
	}

	/**
	 * 使用默认的{@link MockServletContext}.
	 * 
	 * @param filterName 过滤器的名称
	 */
	public MockFilterConfig(String filterName) {
		this(null, filterName);
	}

	/**
	 * @param servletContext 运行servlet的ServletContext
	 */
	public MockFilterConfig(ServletContext servletContext) {
		this(servletContext, "");
	}

	/**
	 * @param servletContext 运行servlet的ServletContext
	 * @param filterName 过滤器的名称
	 */
	public MockFilterConfig(ServletContext servletContext, String filterName) {
		this.servletContext = (servletContext != null ? servletContext : new MockServletContext());
		this.filterName = filterName;
	}


	@Override
	public String getFilterName() {
		return filterName;
	}

	@Override
	public ServletContext getServletContext() {
		return servletContext;
	}

	public void addInitParameter(String name, String value) {
		Assert.notNull(name, "Parameter name must not be null");
		this.initParameters.put(name, value);
	}

	@Override
	public String getInitParameter(String name) {
		Assert.notNull(name, "Parameter name must not be null");
		return this.initParameters.get(name);
	}

	@Override
	public Enumeration<String> getInitParameterNames() {
		return Collections.enumeration(this.initParameters.keySet());
	}

}
