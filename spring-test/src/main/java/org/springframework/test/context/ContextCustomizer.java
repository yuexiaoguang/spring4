package org.springframework.test.context;

import org.springframework.context.ConfigurableApplicationContext;

/**
 * Strategy interface for customizing {@link ConfigurableApplicationContext
 * application contexts} that are created and managed by the <em>Spring
 * TestContext Framework</em>.
 *
 * <p>Customizers are created by {@link ContextCustomizerFactory} implementations.
 *
 * <p>Implementations must implement correct {@code equals} and {@code hashCode}
 * methods since customizers form part of the {@link MergedContextConfiguration}
 * which is used as a cache key.
 */
public interface ContextCustomizer {

	/**
	 * Customize the supplied {@code ConfigurableApplicationContext} <em>after</em>
	 * bean definitions have been loaded into the context but <em>before</em> the
	 * context has been refreshed.
	 * @param context the context to customize
	 * @param mergedConfig the merged context configuration
	 */
	void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig);

}
