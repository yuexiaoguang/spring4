package org.springframework.mock.web;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import javax.el.ELContext;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;

import org.springframework.util.Assert;

/**
 * {@link javax.servlet.jsp.PageContext}接口的模拟实现.
 * 仅在测试自定义JSP标记时测试应用程序所必需的.
 *
 * <p>Note: 通过构造函数而不是通过{@code PageContext.initialize}方法进行初始化.
 * 不支持写入JspWriter, 请求调度或{@code handlePageException}调用.
 */
public class MockPageContext extends PageContext {

	private final ServletContext servletContext;

	private final HttpServletRequest request;

	private final HttpServletResponse response;

	private final ServletConfig servletConfig;

	private final Map<String, Object> attributes = new LinkedHashMap<String, Object>();

	private JspWriter out;


	/**
	 * 使用默认的{@link MockServletContext}, {@link MockHttpServletRequest},
	 * {@link MockHttpServletResponse}, {@link MockServletConfig}.
	 */
	public MockPageContext() {
		this(null, null, null, null);
	}

	/**
	 * 使用默认的{@link MockHttpServletRequest}, {@link MockHttpServletResponse}, {@link MockServletConfig}.
	 * 
	 * @param servletContext 运行JSP页面的ServletContext (仅在实际访问ServletContext时才需要)
	 */
	public MockPageContext(ServletContext servletContext) {
		this(servletContext, null, null, null);
	}

	/**
	 * @param servletContext 运行JSP页面的ServletContext
	 * @param request 当前HttpServletRequest (仅在实际访问请求时才需要)
	 */
	public MockPageContext(ServletContext servletContext, HttpServletRequest request) {
		this(servletContext, request, null, null);
	}

	/**
	 * @param servletContext 运行JSP页面的ServletContext
	 * @param request 当前HttpServletRequest
	 * @param response 当前HttpServletResponse (只有在实际写入响应时才需要)
	 */
	public MockPageContext(ServletContext servletContext, HttpServletRequest request, HttpServletResponse response) {
		this(servletContext, request, response, null);
	}

	/**
	 * @param servletContext 运行JSP页面的ServletContext
	 * @param request 当前HttpServletRequest
	 * @param response 当前HttpServletResponse
	 * @param servletConfig the ServletConfig (几乎没有从标签内访问过)
	 */
	public MockPageContext(ServletContext servletContext, HttpServletRequest request,
			HttpServletResponse response, ServletConfig servletConfig) {

		this.servletContext = (servletContext != null ? servletContext : new MockServletContext());
		this.request = (request != null ? request : new MockHttpServletRequest(servletContext));
		this.response = (response != null ? response : new MockHttpServletResponse());
		this.servletConfig = (servletConfig != null ? servletConfig : new MockServletConfig(servletContext));
	}


	@Override
	public void initialize(
			Servlet servlet, ServletRequest request, ServletResponse response,
			String errorPageURL, boolean needsSession, int bufferSize, boolean autoFlush) {

		throw new UnsupportedOperationException("Use appropriate constructor");
	}

	@Override
	public void release() {
	}

	@Override
	public void setAttribute(String name, Object value) {
		Assert.notNull(name, "Attribute name must not be null");
		if (value != null) {
			this.attributes.put(name, value);
		}
		else {
			this.attributes.remove(name);
		}
	}

	@Override
	public void setAttribute(String name, Object value, int scope) {
		Assert.notNull(name, "Attribute name must not be null");
		switch (scope) {
			case PAGE_SCOPE:
				setAttribute(name, value);
				break;
			case REQUEST_SCOPE:
				this.request.setAttribute(name, value);
				break;
			case SESSION_SCOPE:
				this.request.getSession().setAttribute(name, value);
				break;
			case APPLICATION_SCOPE:
				this.servletContext.setAttribute(name, value);
				break;
			default:
				throw new IllegalArgumentException("Invalid scope: " + scope);
		}
	}

	@Override
	public Object getAttribute(String name) {
		Assert.notNull(name, "Attribute name must not be null");
		return this.attributes.get(name);
	}

	@Override
	public Object getAttribute(String name, int scope) {
		Assert.notNull(name, "Attribute name must not be null");
		switch (scope) {
			case PAGE_SCOPE:
				return getAttribute(name);
			case REQUEST_SCOPE:
				return this.request.getAttribute(name);
			case SESSION_SCOPE:
				HttpSession session = this.request.getSession(false);
				return (session != null ? session.getAttribute(name) : null);
			case APPLICATION_SCOPE:
				return this.servletContext.getAttribute(name);
			default:
				throw new IllegalArgumentException("Invalid scope: " + scope);
		}
	}

	@Override
	public Object findAttribute(String name) {
		Object value = getAttribute(name);
		if (value == null) {
			value = getAttribute(name, REQUEST_SCOPE);
			if (value == null) {
				value = getAttribute(name, SESSION_SCOPE);
				if (value == null) {
					value = getAttribute(name, APPLICATION_SCOPE);
				}
			}
		}
		return value;
	}

	@Override
	public void removeAttribute(String name) {
		Assert.notNull(name, "Attribute name must not be null");
		this.removeAttribute(name, PageContext.PAGE_SCOPE);
		this.removeAttribute(name, PageContext.REQUEST_SCOPE);
		this.removeAttribute(name, PageContext.SESSION_SCOPE);
		this.removeAttribute(name, PageContext.APPLICATION_SCOPE);
	}

	@Override
	public void removeAttribute(String name, int scope) {
		Assert.notNull(name, "Attribute name must not be null");
		switch (scope) {
			case PAGE_SCOPE:
				this.attributes.remove(name);
				break;
			case REQUEST_SCOPE:
				this.request.removeAttribute(name);
				break;
			case SESSION_SCOPE:
				this.request.getSession().removeAttribute(name);
				break;
			case APPLICATION_SCOPE:
				this.servletContext.removeAttribute(name);
				break;
			default:
				throw new IllegalArgumentException("Invalid scope: " + scope);
		}
	}

	@Override
	public int getAttributesScope(String name) {
		if (getAttribute(name) != null) {
			return PAGE_SCOPE;
		}
		else if (getAttribute(name, REQUEST_SCOPE) != null) {
			return REQUEST_SCOPE;
		}
		else if (getAttribute(name, SESSION_SCOPE) != null) {
			return SESSION_SCOPE;
		}
		else if (getAttribute(name, APPLICATION_SCOPE) != null) {
			return APPLICATION_SCOPE;
		}
		else {
			return 0;
		}
	}

	public Enumeration<String> getAttributeNames() {
		return Collections.enumeration(new LinkedHashSet<String>(this.attributes.keySet()));
	}

	@Override
	public Enumeration<String> getAttributeNamesInScope(int scope) {
		switch (scope) {
			case PAGE_SCOPE:
				return getAttributeNames();
			case REQUEST_SCOPE:
				return this.request.getAttributeNames();
			case SESSION_SCOPE:
				HttpSession session = this.request.getSession(false);
				return (session != null ? session.getAttributeNames() : null);
			case APPLICATION_SCOPE:
				return this.servletContext.getAttributeNames();
			default:
				throw new IllegalArgumentException("Invalid scope: " + scope);
		}
	}

	@Override
	public JspWriter getOut() {
		if (this.out == null) {
			this.out = new MockJspWriter(this.response);
		}
		return this.out;
	}

	@Override
	@Deprecated
	public javax.servlet.jsp.el.ExpressionEvaluator getExpressionEvaluator() {
		return new MockExpressionEvaluator(this);
	}

	@Override
	public ELContext getELContext() {
		return null;
	}

	@Override
	@Deprecated
	public javax.servlet.jsp.el.VariableResolver getVariableResolver() {
		return null;
	}

	@Override
	public HttpSession getSession() {
		return this.request.getSession();
	}

	@Override
	public Object getPage() {
		return this;
	}

	@Override
	public ServletRequest getRequest() {
		return this.request;
	}

	@Override
	public ServletResponse getResponse() {
		return this.response;
	}

	@Override
	public Exception getException() {
		return null;
	}

	@Override
	public ServletConfig getServletConfig() {
		return this.servletConfig;
	}

	@Override
	public ServletContext getServletContext() {
		return this.servletContext;
	}

	@Override
	public void forward(String path) throws ServletException, IOException {
		this.request.getRequestDispatcher(path).forward(this.request, this.response);
	}

	@Override
	public void include(String path) throws ServletException, IOException {
		this.request.getRequestDispatcher(path).include(this.request, this.response);
	}

	@Override
	public void include(String path, boolean flush) throws ServletException, IOException {
		this.request.getRequestDispatcher(path).include(this.request, this.response);
		if (flush) {
			this.response.flushBuffer();
		}
	}

	public byte[] getContentAsByteArray() {
		Assert.state(this.response instanceof MockHttpServletResponse, "MockHttpServletResponse required");
		return ((MockHttpServletResponse) this.response).getContentAsByteArray();
	}

	public String getContentAsString() throws UnsupportedEncodingException {
		Assert.state(this.response instanceof MockHttpServletResponse, "MockHttpServletResponse required");
		return ((MockHttpServletResponse) this.response).getContentAsString();
	}

	@Override
	public void handlePageException(Exception ex) throws ServletException, IOException {
		throw new ServletException("Page exception", ex);
	}

	@Override
	public void handlePageException(Throwable ex) throws ServletException, IOException {
		throw new ServletException("Page exception", ex);
	}

}
