package org.springframework.mock.web;

import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.springframework.util.Assert;

/**
 * {@link javax.servlet.ServletConfig}接口的模拟实现.
 *
 * <p>用于测试Web框架; 通常不需要测试应用程序控制器.
 */
public class MockServletConfig implements ServletConfig {

	private final ServletContext servletContext;

	private final String servletName;

	private final Map<String, String> initParameters = new LinkedHashMap<String, String>();


	/**
	 * 使用默认的{@link MockServletContext}.
	 */
	public MockServletConfig() {
		this(null, "");
	}

	/**
	 * 使用默认的{@link MockServletContext}.
	 * @param servletName servlet的名称
	 */
	public MockServletConfig(String servletName) {
		this(null, servletName);
	}

	/**
	 * @param servletContext 运行servlet的ServletContext
	 */
	public MockServletConfig(ServletContext servletContext) {
		this(servletContext, "");
	}

	/**
	 * @param servletContext 运行servlet的ServletContext
	 * @param servletName servlet的名称
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
