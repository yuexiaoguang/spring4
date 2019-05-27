package org.springframework.web.context.support;

import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.servlet.ServletContext;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.LiveBeansView;
import org.springframework.util.Assert;

/**
 * {@link LiveBeansView} subclass which looks for all ApplicationContexts
 * in the web application, as exposed in ServletContext attributes.
 */
public class ServletContextLiveBeansView extends LiveBeansView {

	private final ServletContext servletContext;

	/**
	 * Create a new LiveBeansView for the given ServletContext.
	 * @param servletContext current ServletContext
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
