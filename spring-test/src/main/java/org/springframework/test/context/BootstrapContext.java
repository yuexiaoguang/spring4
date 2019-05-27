package org.springframework.test.context;

/**
 * {@code BootstrapContext} encapsulates the context in which the <em>Spring
 * TestContext Framework</em> is bootstrapped.
 */
public interface BootstrapContext {

	/**
	 * Get the {@linkplain Class test class} for this bootstrap context.
	 * @return the test class (never {@code null})
	 */
	Class<?> getTestClass();

	/**
	 * Get the {@link CacheAwareContextLoaderDelegate} to use for transparent
	 * interaction with the {@code ContextCache}.
	 * @return the context loader delegate (never {@code null})
	 */
	CacheAwareContextLoaderDelegate getCacheAwareContextLoaderDelegate();

}
