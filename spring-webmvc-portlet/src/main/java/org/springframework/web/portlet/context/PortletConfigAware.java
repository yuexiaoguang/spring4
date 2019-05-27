package org.springframework.web.portlet.context;

import javax.portlet.PortletConfig;

import org.springframework.beans.factory.Aware;

/**
 * Interface to be implemented by any object that wishes to be notified
 * of the PortletConfig (typically determined by the PortletApplicationContext)
 * that it runs in.
 */
public interface PortletConfigAware extends Aware {

	/**
	 * Set the PortletConfigthat this object runs in.
	 * <p>Invoked after population of normal bean properties but before an init
	 * callback like InitializingBean's afterPropertiesSet or a custom init-method.
	 * Invoked after ApplicationContextAware's setApplicationContext.
	 * @param portletConfig PortletConfig object to be used by this object
	 */
	void setPortletConfig(PortletConfig portletConfig);

}
