package org.springframework.web.context;

import javax.servlet.ServletConfig;

import org.springframework.beans.factory.Aware;

/**
 * Interface to be implemented by any object that wishes to be notified of the
 * {@link ServletConfig} (typically determined by the {@link WebApplicationContext})
 * that it runs in.
 *
 * <p>Note: Only satisfied if actually running within a Servlet-specific
 * WebApplicationContext. Otherwise, no ServletConfig will be set.
 */
public interface ServletConfigAware extends Aware {

	/**
	 * Set the {@link ServletConfig} that this object runs in.
	 * <p>Invoked after population of normal bean properties but before an init
	 * callback like InitializingBean's {@code afterPropertiesSet} or a
	 * custom init-method. Invoked after ApplicationContextAware's
	 * {@code setApplicationContext}.
	 * @param servletConfig ServletConfig object to be used by this object
	 */
	void setServletConfig(ServletConfig servletConfig);

}
