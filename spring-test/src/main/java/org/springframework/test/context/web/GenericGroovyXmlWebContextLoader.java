package org.springframework.test.context.web;

import org.springframework.beans.factory.groovy.GroovyBeanDefinitionReader;
import org.springframework.web.context.support.GenericWebApplicationContext;

/**
 * Concrete implementation of {@link AbstractGenericWebContextLoader} that loads
 * bean definitions from Groovy scripts <em>and</em> XML configuration files.
 *
 * <p>Default resource locations are detected using the suffixes
 * {@code "-context.xml"} and {@code "Context.groovy"}.
 */
public class GenericGroovyXmlWebContextLoader extends GenericXmlWebContextLoader {

	/**
	 * Load bean definitions into the supplied {@link GenericWebApplicationContext context}
	 * from the locations in the supplied {@code WebMergedContextConfiguration} using a
	 * {@link GroovyBeanDefinitionReader}.
	 * @param context the context into which the bean definitions should be loaded
	 * @param webMergedConfig the merged context configuration
	 */
	@Override
	protected void loadBeanDefinitions(GenericWebApplicationContext context,
			WebMergedContextConfiguration webMergedConfig) {
		new GroovyBeanDefinitionReader(context).loadBeanDefinitions(webMergedConfig.getLocations());
	}

	/**
	 * Returns {@code "-context.xml" and "Context.groovy"} in order to
	 * support detection of a default XML config file or Groovy script.
	 */
	@Override
	protected String[] getResourceSuffixes() {
		return new String[] { super.getResourceSuffix(), "Context.groovy" };
	}

	/**
	 * {@code GenericGroovyXmlWebContextLoader} supports both Groovy and XML
	 * resource types for detection of defaults. Consequently, this method
	 * is not supported.
	 * @see #getResourceSuffixes()
	 * @throws UnsupportedOperationException in this implementation
	 */
	@Override
	protected String getResourceSuffix() {
		throw new UnsupportedOperationException(
				"GenericGroovyXmlWebContextLoader does not support the getResourceSuffix() method");
	}

}
