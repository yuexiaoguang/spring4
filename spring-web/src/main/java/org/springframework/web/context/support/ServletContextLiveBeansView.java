package org.springframework.web.context.support;

import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.servlet.ServletContext;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.LiveBeansView;
import org.springframework.util.Assert;

/**
 * {@link LiveBeansView}子类, 它在Web应用程序中查找所有ApplicationContexts, 在ServletContext属性中公开.
 */
public class ServletContextLiveBeansView extends LiveBeansView {

	private final ServletContext servletContext;

	/**
	 * @param servletContext 当前ServletContext
	 */
	public ServletContextLiveBeansView(ServletContext servletContext) {
		Assert.notNull(servletContext, "ServletContext must not be null");
		this.servletContext = servletContext;
	}

	@Override
	protected Set<ConfigurableApplicationContext> findApplicationContexts() {
		Set<ConfigurableApplicationContext> contexts = new LinkedHashSet<ConfigurableApplicationContext>();
		Enumeration<String> attrNames = this.servletContext.getAttributeNames();
		while (attrNames.hasMoreElements()) {
			String attrName = attrNames.nextElement();
			Object attrValue = this.servletContext.getAttribute(attrName);
			if (attrValue instanceof ConfigurableApplicationContext) {
				contexts.add((ConfigurableApplicationContext) attrValue);
			}
		}
		return contexts;
	}

}
