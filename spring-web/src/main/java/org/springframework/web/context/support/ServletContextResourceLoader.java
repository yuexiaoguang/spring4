package org.springframework.web.context.support;

import javax.servlet.ServletContext;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

/**
 * ResourceLoader实现, 将路径解析为ServletContext资源, 以便在WebApplicationContext外部使用
 * (例如, 在HttpServletBean或GenericFilterBean子类中).
 *
 * <p>在WebApplicationContext中, 资源路径由上下文实现自动解析为ServletContext资源.
 */
public class ServletContextResourceLoader extends DefaultResourceLoader {

	private final ServletContext servletContext;


	/**
	 * @param servletContext 用于加载资源的ServletContext
	 */
	public ServletContextResourceLoader(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	/**
	 * 此实现支持Web应用程序根目录下的文件路径.
	 */
	@Override
	protected Resource getResourceByPath(String path) {
		return new ServletContextResource(this.servletContext, path);
	}

}
