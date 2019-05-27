package org.springframework.web.portlet.context;

import javax.portlet.PortletContext;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

/**
 * ResourceLoader implementation that resolves paths as PortletContext
 * resources, for use outside a Portlet ApplicationContext (for example,
 * in a GenericPortletBean subclass).
 *
 * <p>Within a WebApplicationContext, resource paths are automatically
 * resolved as PortletContext resources by the context implementation.
 */
public class PortletContextResourceLoader extends DefaultResourceLoader {

	private final PortletContext portletContext;


	/**
	 * Create a new PortletContextResourceLoader.
	 * @param portletContext the PortletContext to load resources with
	 */
	public PortletContextResourceLoader(PortletContext portletContext) {
		this.portletContext = portletContext;
	}

	/**
	 * This implementation supports file paths beneath the root of the web application.
	 */
	@Override
	protected Resource getResourceByPath(String path) {
		return new PortletContextResource(this.portletContext, path);
	}

}
