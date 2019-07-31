package org.springframework.web.context.support;

import java.io.File;
import javax.servlet.ServletContext;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.util.WebUtils;

/**
 * 在{@link WebApplicationContext}中运行的应用程序对象的便捷超类.
 * 提供{@code getWebApplicationContext()}, {@code getServletContext()}, 和{@code getTempDir()}访问器.
 *
 * <p>Note: 通常建议对所需的实际回调使用单独的回调接口.
 * 这个广泛的基类主要用于框架内, 通常{@link ServletContext}访问等需要.
 */
public abstract class WebApplicationObjectSupport extends ApplicationObjectSupport implements ServletContextAware {

	private ServletContext servletContext;


	@Override
	public final void setServletContext(ServletContext servletContext) {
		if (servletContext != this.servletContext) {
			this.servletContext = servletContext;
			if (servletContext != null) {
				initServletContext(servletContext);
			}
		}
	}

	/**
	 * 覆盖基类行为以强制在ApplicationContext中运行.
	 * 如果未在上下文中运行, 则所有访问器都将抛出IllegalStateException.
	 */
	@Override
	protected boolean isContextRequired() {
		return true;
	}

	/**
	 * 调用{@link #initServletContext(javax.servlet.ServletContext)},
	 * 如果给定的ApplicationContext是{@link WebApplicationContext}.
	 */
	@Override
	protected void initApplicationContext(ApplicationContext context) {
		super.initApplicationContext(context);
		if (this.servletContext == null && context instanceof WebApplicationContext) {
			this.servletContext = ((WebApplicationContext) context).getServletContext();
			if (this.servletContext != null) {
				initServletContext(this.servletContext);
			}
		}
	}

	/**
	 * 子类可以基于此应用程序对象运行的ServletContext重写此自定义初始化.
	 * <p>默认实现为空.
	 * 由{@link #initApplicationContext(org.springframework.context.ApplicationContext)}
	 * 和{@link #setServletContext(javax.servlet.ServletContext)}调用.
	 * 
	 * @param servletContext 此应用程序对象运行的ServletContext (never {@code null})
	 */
	protected void initServletContext(ServletContext servletContext) {
	}

	/**
	 * 返回当前应用程序上下文.
	 * <p><b>NOTE:</b> 如果确实需要访问特定于WebApplicationContext的功能, 才使用它.
	 * 最好使用{@code getApplicationContext()}或{@code getServletContext()},
	 * 以便能够在非WebApplicationContext环境中运行.
	 * 
	 * @throws IllegalStateException 如果没有在WebApplicationContext中运行
	 */
	protected final WebApplicationContext getWebApplicationContext() throws IllegalStateException {
		ApplicationContext ctx = getApplicationContext();
		if (ctx instanceof WebApplicationContext) {
			return (WebApplicationContext) getApplicationContext();
		}
		else if (isContextRequired()) {
			throw new IllegalStateException("WebApplicationObjectSupport instance [" + this +
					"] does not run in a WebApplicationContext but in: " + ctx);
		}
		else {
			return null;
		}
	}

	/**
	 * 返回当前的ServletContext.
	 * 
	 * @throws IllegalStateException 如果没有在ServletContext中运行
	 */
	protected final ServletContext getServletContext() throws IllegalStateException {
		if (this.servletContext != null) {
			return this.servletContext;
		}
		WebApplicationContext wac = getWebApplicationContext();
		if (wac == null) {
			return null;
		}
		ServletContext servletContext = wac.getServletContext();
		if (servletContext == null && isContextRequired()) {
			throw new IllegalStateException("WebApplicationObjectSupport instance [" + this +
					"] does not run within a ServletContext. Make sure the object is fully configured!");
		}
		return servletContext;
	}

	/**
	 * 返回当前Web应用程序的临时目录, 由servlet容器提供.
	 * 
	 * @return 表示临时目录的文件
	 * @throws IllegalStateException 如果没有在ServletContext中运行
	 */
	protected final File getTempDir() throws IllegalStateException {
		return WebUtils.getTempDir(getServletContext());
	}

}
