package org.springframework.web.servlet.resource;

import java.io.IOException;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.util.StringUtils;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.context.ServletContextAware;

/**
 * {@link HttpRequestHandler}, 用于使用Servlet容器的"default" Servlet提供静态文件.
 *
 * <p>当{@link org.springframework.web.servlet.DispatcherServlet DispatcherServlet}
 * 映射到"/"时, 此处理程序旨在与"/*"映射一起使用, 从而覆盖Servlet容器对静态资源的默认处理.
 * 通常应该将此处理器的映射排序为链中的最后一个, 以便只有在没有其他更具体的映射 (i.e., 到控制器)匹配时才会执行.
 *
 * <p>通过{@link RequestDispatcher}转发来处理请求, 该{@link RequestDispatcher}通过
 * {@link #setDefaultServletName "defaultServletName" 属性}指定的名称获取.
 * 在大多数情况下, {@code defaultServletName}不需要显式设置,
 * 因为处理器在初始化时检查是否存在Tomcat, Jetty, Resin, WebLogic 和 WebSphere等知名容器的默认Servlet.
 * 但是, 在默认Servlet名称未知的容器中运行, 或者通过服务器配置自定义的位置运行时, 需要明确设置{@code defaultServletName}.
 */
public class DefaultServletHttpRequestHandler implements HttpRequestHandler, ServletContextAware {

	/** Tomcat, Jetty, JBoss, 和 GlassFish使用的默认Servlet名称 */
	private static final String COMMON_DEFAULT_SERVLET_NAME = "default";

	/** Google App Engine使用的默认Servlet名称 */
	private static final String GAE_DEFAULT_SERVLET_NAME = "_ah_default";

	/** Resin使用的默认Servlet名称 */
	private static final String RESIN_DEFAULT_SERVLET_NAME = "resin-file";

	/** WebLogic使用的默认Servlet名称 */
	private static final String WEBLOGIC_DEFAULT_SERVLET_NAME = "FileServlet";

	/** WebSphere使用的默认Servlet名称 */
	private static final String WEBSPHERE_DEFAULT_SERVLET_NAME = "SimpleFileServlet";


	private String defaultServletName;

	private ServletContext servletContext;


	/**
	 * 为静态资源请求设置要转发的默认Servlet的名称.
	 */
	public void setDefaultServletName(String defaultServletName) {
		this.defaultServletName = defaultServletName;
	}

	/**
	 * 如果未显式设置{@code defaultServletName}属性, 则尝试使用已知的常见容器特定名称来定位默认Servlet.
	 */
	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
		if (!StringUtils.hasText(this.defaultServletName)) {
			if (this.servletContext.getNamedDispatcher(COMMON_DEFAULT_SERVLET_NAME) != null) {
				this.defaultServletName = COMMON_DEFAULT_SERVLET_NAME;
			}
			else if (this.servletContext.getNamedDispatcher(GAE_DEFAULT_SERVLET_NAME) != null) {
				this.defaultServletName = GAE_DEFAULT_SERVLET_NAME;
			}
			else if (this.servletContext.getNamedDispatcher(RESIN_DEFAULT_SERVLET_NAME) != null) {
				this.defaultServletName = RESIN_DEFAULT_SERVLET_NAME;
			}
			else if (this.servletContext.getNamedDispatcher(WEBLOGIC_DEFAULT_SERVLET_NAME) != null) {
				this.defaultServletName = WEBLOGIC_DEFAULT_SERVLET_NAME;
			}
			else if (this.servletContext.getNamedDispatcher(WEBSPHERE_DEFAULT_SERVLET_NAME) != null) {
				this.defaultServletName = WEBSPHERE_DEFAULT_SERVLET_NAME;
			}
			else {
				throw new IllegalStateException("Unable to locate the default servlet for serving static content. " +
						"Please set the 'defaultServletName' property explicitly.");
			}
		}
	}


	@Override
	public void handleRequest(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		RequestDispatcher rd = this.servletContext.getNamedDispatcher(this.defaultServletName);
		if (rd == null) {
			throw new IllegalStateException("A RequestDispatcher could not be located for the default servlet '" +
					this.defaultServletName + "'");
		}
		rd.forward(request, response);
	}

}
