package org.springframework.web.portlet.context;

import javax.portlet.PortletContext;

import org.springframework.beans.factory.Aware;

/**
 * Interface to be implemented by any object that wishes to be notified
 * of the PortletContext (typically determined by the PortletApplicationContext)
 * that it runs in.
 */
public interface PortletContextAware extends Aware {

	/**
	 * Set the PortletContext that this object runs in.
	 * <p>Invoked after population of normal bean properties but before an init
	 * callback like InitializingBean's afterPropertiesSet or a custom init-method.
	 * Invoked after ApplicationContextAware's setApplicationContext.
	 * @param portletContext PortletContext object to be used by this object
	 */
	void setPortletContext(PortletContext portletContext);

}
