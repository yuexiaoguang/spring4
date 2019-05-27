package org.springframework.web.servlet.view.freemarker;

import freemarker.ext.jsp.TaglibFactory;
import freemarker.template.Configuration;

/**
 * Interface to be implemented by objects that configure and manage a
 * FreeMarker Configuration object in a web environment. Detected and
 * used by {@link FreeMarkerView}.
 */
public interface FreeMarkerConfig {

	/**
	 * Return the FreeMarker {@link Configuration} object for the current
	 * web application context.
	 * <p>A FreeMarker Configuration object may be used to set FreeMarker
	 * properties and shared objects, and allows to retrieve templates.
	 * @return the FreeMarker Configuration
	 */
	Configuration getConfiguration();

	/**
	 * Return the {@link TaglibFactory} used to enable JSP tags to be
	 * accessed from FreeMarker templates.
	 */
	TaglibFactory getTaglibFactory();

}
