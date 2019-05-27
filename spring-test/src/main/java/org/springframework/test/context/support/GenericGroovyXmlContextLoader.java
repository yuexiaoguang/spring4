package org.springframework.test.context.support;

import org.springframework.beans.factory.groovy.GroovyBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.context.MergedContextConfiguration;

/**
 * Concrete implementation of {@link AbstractGenericContextLoader} that reads
 * bean definitions from Groovy scripts <em>and</em> XML configuration files.
 *
 * <p>Default resource locations are detected using the suffixes
 * {@code "-context.xml"} and {@code "Context.groovy"}.
 */
public class GenericGroovyXmlContextLoader extends GenericXmlContextLoader {

	/**
	 * Load bean definitions into the supplied {@link GenericApplicationContext context}
	 * from the locations in the supplied {@code MergedContextConfiguration} using a
	 * {@link GroovyBeanDefinitionReader}.
	 * @param context the context into which the bean definitions should be loaded
	 * @param mergedConfig the merged context configuration
	 */
	@Override
	protected void loadBeanDefinitions(GenericApplicationContext context, MergedContextConfiguration mergedConfig) {
		new GroovyBeanDefinitionReader(context).loadBeanDefinitions(mergedConfig.getLocations());
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
	 * {@code GenericGroovyXmlContextLoader} supports both Groovy and XML
	 * resource types for detection of defaults. Consequently, this method
	 * is not supported.
	 * @see #getResourceSuffixes()
	 * @throws UnsupportedOperationException in this implementation
	 */
	@Override
	protected String getResourceSuffix() {
		throw new UnsupportedOperationException(
				"GenericGroovyXmlContextLoader does not support the getResourceSuffix() method");
	}

}
