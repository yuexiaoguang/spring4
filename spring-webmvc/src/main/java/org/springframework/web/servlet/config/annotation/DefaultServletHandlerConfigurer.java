package org.springframework.web.servlet.config.annotation;

import java.util.Collections;
import javax.servlet.ServletContext;

import org.springframework.util.Assert;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.resource.DefaultServletHttpRequestHandler;

/**
 * 通过将请求转发到Servlet容器的"default" Servlet, 配置用于提供静态资源的请求处理器.
 * 这是在Spring MVC {@link DispatcherServlet}映射到 "/"时使用, 从而覆盖Servlet容器对静态资源的默认处理.
 *
 * <p>由于此处理器是以最低优先级配置的, 因此它实际上允许所有其他处理器映射来处理请求,
 * 如果它们都不执行, 则此处理器可以将其转发到"默认" Servlet.
 */
public class DefaultServletHandlerConfigurer {

	private final ServletContext servletContext;

	private DefaultServletHttpRequestHandler handler;


	public DefaultServletHandlerConfigurer(ServletContext servletContext) {
		Assert.notNull(servletContext, "ServletContext is required");
		this.servletContext = servletContext;
	}


	/**
	 * 启用转发到"默认" Servlet.
	 * <p>使用此方法时, {@link DefaultServletHttpRequestHandler}将尝试自动检测"默认" Servlet名称.
	 * 或者, 可以通过{@link #enable(String)}指定默认Servlet的名称.
	 */
	public void enable() {
		enable(null);
	}

	/**
	 * 启用转发到由给定名称标识的"默认" Servlet.
	 * <p>当无法自动检测默认Servlet时, 例如已经手动配置了它, 这非常有用.
	 */
	public void enable(String defaultServletName) {
		this.handler = new DefaultServletHttpRequestHandler();
		this.handler.setDefaultServletName(defaultServletName);
		this.handler.setServletContext(this.servletContext);
	}


	/**
	 * 返回在{@link Integer#MAX_VALUE}处排序的处理器映射实例,
	 * 其中包含映射到{@code "/**"}的{@link DefaultServletHttpRequestHandler}实例;
	 * 或{@code null}如果未启用默认servlet处理.
	 */
	protected SimpleUrlHandlerMapping buildHandlerMapping() {
		if (this.handler == null) {
			return null;
		}

		SimpleUrlHandlerMapping handlerMapping = new SimpleUrlHandlerMapping();
		handlerMapping.setUrlMap(Collections.singletonMap("/**", this.handler));
		handlerMapping.setOrder(Integer.MAX_VALUE);
		return handlerMapping;
	}

	/**
	 * @deprecated as of 4.3.12, in favor of {@link #buildHandlerMapping()}
	 */
	@Deprecated
	protected AbstractHandlerMapping getHandlerMapping() {
		return buildHandlerMapping();
	}

}
