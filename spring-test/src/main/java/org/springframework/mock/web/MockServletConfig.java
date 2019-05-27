package org.springframework.mock.web;

import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.springframework.util.Assert;

/**
 * Mock implementation of the {@link javax.servlet.ServletConfig} interface.
 *
 * <p>Used for testing the web framework; typically not necessary for
 * testing application controllers.
 */
public class MockServletConfig implements ServletConfig {

	private final ServletContext servletContext;

	private final String servletName;

	private final Map<String, String> initParameters = new LinkedHashMap<String, String>();


	/**
	 * Create a new MockServletConfig with a default {@link MockServletContext}.
	 */
	public MockServletConfig() {
		this(null, "");
	}

	/**
	 * Create a new MockServletConfig with a default {@link MockServletContext}.
	 * @param servletName the name of the servlet
	 */
	public MockServletConfig(String servletName) {
		this(null, servletName);
	}

	/**
	 * Create a new MockServletConfig.
	 * @param servletContext the ServletContext that the servlet runs in
	 */
	public MockServletConfig(ServletContext servletContext) {
		this(servletContext, "");
	}

	/**
	 * Create a new MockServletConfig.
	 * @param servletContext the ServletContext that the servlet runs in
	 * @param servletName the name of the servlet
	 */
	public MockServletConfig(ServletContext servletContext, String servletName) {
		this.servletContext = (servletContext != null ? servletContext : new MockServletContext());
		this.servletName = servletName;
	}


	@Override
	public String getServletName() {
		return this.servletName;
	}

	@Override
	public ServletContext getServletContext() {
		return this.servletContext;
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
