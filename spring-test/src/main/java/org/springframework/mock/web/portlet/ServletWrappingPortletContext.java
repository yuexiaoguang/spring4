package org.springframework.mock.web.portlet;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Set;
import javax.portlet.PortletContext;
import javax.portlet.PortletRequestDispatcher;
import javax.servlet.ServletContext;

import org.springframework.util.Assert;

/**
 * {@link javax.portlet.PortletContext}接口的模拟实现, 包装底层{@link javax.servlet.ServletContext}.
 */
public class ServletWrappingPortletContext implements PortletContext {

	private final ServletContext servletContext;


	/**
	 * @param servletContext 要包装的ServletContext
	 */
	public ServletWrappingPortletContext(ServletContext servletContext) {
		Assert.notNull(servletContext, "ServletContext must not be null");
		this.servletContext = servletContext;
	}

	/**
	 * 返回此PortletContext包装的底层ServletContext.
	 */
	public final ServletContext getServletContext() {
		return this.servletContext;
	}


	@Override
	public String getServerInfo() {
		return this.servletContext.getServerInfo();
	}

	@Override
	public PortletRequestDispatcher getRequestDispatcher(String path) {
		return null;
	}

	@Override
	public PortletRequestDispatcher getNamedDispatcher(String name) {
		return null;
	}

	@Override
	public InputStream getResourceAsStream(String path) {
		return this.servletContext.getResourceAsStream(path);
	}

	@Override
	public int getMajorVersion() {
		return 2;
	}

	@Override
	public int getMinorVersion() {
		return 0;
	}

	@Override
	public String getMimeType(String file) {
		return this.servletContext.getMimeType(file);
	}

	@Override
	public String getRealPath(String path) {
		return this.servletContext.getRealPath(path);
	}

	@Override
	public Set<String> getResourcePaths(String path) {
		return this.servletContext.getResourcePaths(path);
	}

	@Override
	public URL getResource(String path) throws MalformedURLException {
		return this.servletContext.getResource(path);
	}

	@Override
	public Object getAttribute(String name) {
		return this.servletContext.getAttribute(name);
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		return this.servletContext.getAttributeNames();
	}

	@Override
	public String getInitParameter(String name) {
		return this.servletContext.getInitParameter(name);
	}

	@Override
	public Enumeration<String> getInitParameterNames() {
		return this.servletContext.getInitParameterNames();
	}

	@Override
	public void log(String msg) {
		this.servletContext.log(msg);
	}

	@Override
	public void log(String message, Throwable throwable) {
		this.servletContext.log(message, throwable);
	}

	@Override
	public void removeAttribute(String name) {
		this.servletContext.removeAttribute(name);
	}

	@Override
	public void setAttribute(String name, Object object) {
		this.servletContext.setAttribute(name, object);
	}

	@Override
	public String getPortletContextName() {
		return this.servletContext.getServletContextName();
	}

	@Override
	public Enumeration<String> getContainerRuntimeOptions() {
		return Collections.enumeration(Collections.<String>emptySet());
	}

}
