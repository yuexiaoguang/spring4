package org.springframework.test.web.servlet.setup;

import javax.servlet.ServletContext;

import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * {@link AbstractMockMvcBuilder}的具体实现, 提供作为构造函数参数提供给它的{@link WebApplicationContext}.
 *
 * <p>此外, 如果提供的{@code WebApplicationContext}中的{@link ServletContext}不包含
 * {@link WebApplicationContext#ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE} 键的条目,
 * 则将检测根{@code WebApplicationContext}, 并将其存储在{@code ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE}键下的{@code ServletContext}.
 */
public class DefaultMockMvcBuilder extends AbstractMockMvcBuilder<DefaultMockMvcBuilder> {

	private final WebApplicationContext webAppContext;


	/**
	 * 不适用于直接实例化.
	 */
	protected DefaultMockMvcBuilder(WebApplicationContext webAppContext) {
		Assert.notNull(webAppContext, "WebApplicationContext is required");
		Assert.notNull(webAppContext.getServletContext(), "WebApplicationContext must have a ServletContext");
		this.webAppContext = webAppContext;
	}

	@Override
	protected WebApplicationContext initWebAppContext() {

		ServletContext servletContext = this.webAppContext.getServletContext();
		ApplicationContext rootWac = WebApplicationContextUtils.getWebApplicationContext(servletContext);

		if (rootWac == null) {
			rootWac = this.webAppContext;
			ApplicationContext parent = this.webAppContext.getParent();
			while (parent != null) {
				if (parent instanceof WebApplicationContext && !(parent.getParent() instanceof WebApplicationContext)) {
					rootWac = parent;
					break;
				}
				parent = parent.getParent();
			}
			servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, rootWac);
		}

		return this.webAppContext;
	}

}
