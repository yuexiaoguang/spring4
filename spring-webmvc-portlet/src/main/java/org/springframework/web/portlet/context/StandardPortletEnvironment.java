package org.springframework.web.portlet.context;

import javax.portlet.PortletConfig;
import javax.portlet.PortletContext;
import javax.servlet.ServletContext;

import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySource.StubPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.jndi.JndiLocatorDelegate;
import org.springframework.jndi.JndiPropertySource;
import org.springframework.web.context.support.StandardServletEnvironment;

/**
 * {@link Environment} implementation to be used by {@code Servlet}-based web
 * applications. All Portlet-related {@code ApplicationContext} classes
 * initialize an instance by default.
 *
 * <p>Contributes {@code ServletContext}, {@code PortletContext},
 * {@code PortletConfig} and JNDI-based {@link PropertySource} instances.
 * See the {@link #customizePropertySources} method for details.
 */
public class StandardPortletEnvironment extends StandardEnvironment {

	/** Portlet context init parameters property source name: {@value} */
	public static final String PORTLET_CONTEXT_PROPERTY_SOURCE_NAME = "portletContextInitParams";

	/** Portlet config init parameters property source name: {@value} */
	public static final String PORTLET_CONFIG_PROPERTY_SOURCE_NAME = "portletConfigInitParams";


	/**
	 * Customize the set of property sources with those contributed by superclasses as
	 * well as those appropriate for standard portlet-based environments:
	 * <ul>
	 * <li>{@value #PORTLET_CONFIG_PROPERTY_SOURCE_NAME}
	 * <li>{@value #PORTLET_CONTEXT_PROPERTY_SOURCE_NAME}
	 * <li>{@linkplain StandardServletEnvironment#SERVLET_CONTEXT_PROPERTY_SOURCE_NAME "servletContextInitParams"}
	 * <li>{@linkplain StandardServletEnvironment#JNDI_PROPERTY_SOURCE_NAME "jndiProperties"}
	 * </ul>
	 * <p>Properties present in {@value #PORTLET_CONFIG_PROPERTY_SOURCE_NAME} will
	 * take precedence over those in {@value #PORTLET_CONTEXT_PROPERTY_SOURCE_NAME},
	 * which takes precedence over those in {@linkplain
	 * StandardServletEnvironment#SERVLET_CONTEXT_PROPERTY_SOURCE_NAME "servletContextInitParams"}
	 * and so on.
	 * <p>Properties in any of the above will take precedence over system properties and
	 * environment variables contributed by the {@link StandardEnvironment} superclass.
	 * <p>The property sources are added as stubs for now, and will be
	 * {@linkplain PortletApplicationContextUtils#initPortletPropertySources fully
	 * initialized} once the actual {@link PortletConfig}, {@link PortletContext}, and
	 * {@link ServletContext} objects are available.
	 */
	@Override
	protected void customizePropertySources(MutablePropertySources propertySources) {
		propertySources.addLast(new StubPropertySource(PORTLET_CONFIG_PROPERTY_SOURCE_NAME));
		propertySources.addLast(new StubPropertySource(PORTLET_CONTEXT_PROPERTY_SOURCE_NAME));
		propertySources.addLast(new StubPropertySource(StandardServletEnvironment.SERVLET_CONTEXT_PROPERTY_SOURCE_NAME));
		if (JndiLocatorDelegate.isDefaultJndiEnvironmentAvailable()) {
			propertySources.addLast(new JndiPropertySource(StandardServletEnvironment.JNDI_PROPERTY_SOURCE_NAME));
		}
		super.customizePropertySources(propertySources);
	}

	/**
	 * Replace any {@linkplain
	 * org.springframework.core.env.PropertySource.StubPropertySource stub property source}
	 * instances acting as placeholders with real portlet context/config property sources
	 * using the given parameters.
	 * @param servletContext the {@link ServletContext} (may be {@code null})
	 * @param portletContext the {@link PortletContext} (may not be {@code null})
	 * @param portletConfig the {@link PortletConfig} ({@code null} if not available)
	 */
	public void initPropertySources(ServletContext servletContext, PortletContext portletContext, PortletConfig portletConfig) {
		PortletApplicationContextUtils.initPortletPropertySources(getPropertySources(), servletContext, portletContext, portletConfig);
	}

}