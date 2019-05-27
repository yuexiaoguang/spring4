package org.springframework.test.context.support;

import org.springframework.core.style.ToStringCreator;
import org.springframework.test.context.BootstrapContext;
import org.springframework.test.context.CacheAwareContextLoaderDelegate;
import org.springframework.util.Assert;

/**
 * Default implementation of the {@link BootstrapContext} interface.
 */
public class DefaultBootstrapContext implements BootstrapContext {

	private final Class<?> testClass;
	private final CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate;


	/**
	 * Construct a new {@code DefaultBootstrapContext} from the supplied arguments.
	 * @param testClass the test class for this bootstrap context; never {@code null}
	 * @param cacheAwareContextLoaderDelegate the context loader delegate to use for
	 * transparent interaction with the {@code ContextCache}; never {@code null}
	 */
	public DefaultBootstrapContext(Class<?> testClass, CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate) {
		Assert.notNull(testClass, "Test class must not be null");
		Assert.notNull(cacheAwareContextLoaderDelegate, "CacheAwareContextLoaderDelegate must not be null");
		this.testClass = testClass;
		this.cacheAwareContextLoaderDelegate = cacheAwareContextLoaderDelegate;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Class<?> getTestClass() {
		return this.testClass;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public CacheAwareContextLoaderDelegate getCacheAwareContextLoaderDelegate() {
		return this.cacheAwareContextLoaderDelegate;
	}

	/**
	 * Provide a String representation of this bootstrap context's state.
	 */
	@Override
	public String toString() {
		return new ToStringCreator(this)//
		.append("testClass", testClass.getName())//
		.append("cacheAwareContextLoaderDelegate", cacheAwareContextLoaderDelegate.getClass().getName())//
		.toString();
	}

}
