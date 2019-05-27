package org.springframework.test.context;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;

/**
 * A {@code CacheAwareContextLoaderDelegate} is responsible for {@linkplain
 * #loadContext loading} and {@linkplain #closeContext closing} application
 * contexts, interacting transparently with a
 * {@link org.springframework.test.context.cache.ContextCache ContextCache}
 * behind the scenes.
 *
 * <p>Note: {@code CacheAwareContextLoaderDelegate} does not extend the
 * {@link ContextLoader} or {@link SmartContextLoader} interface.
 */
public interface CacheAwareContextLoaderDelegate {

	/**
	 * Load the {@linkplain ApplicationContext application context} for the supplied
	 * {@link MergedContextConfiguration} by delegating to the {@link ContextLoader}
	 * configured in the given {@code MergedContextConfiguration}.
	 * <p>If the context is present in the {@code ContextCache} it will simply
	 * be returned; otherwise, it will be loaded, stored in the cache, and returned.
	 * <p>The cache statistics should be logged by invoking
	 * {@link org.springframework.test.context.cache.ContextCache#logStatistics()}.
	 * @param mergedContextConfiguration the merged context configuration to use
	 * to load the application context; never {@code null}
	 * @return the application context
	 * @throws IllegalStateException if an error occurs while retrieving or loading
	 * the application context
	 */
	ApplicationContext loadContext(MergedContextConfiguration mergedContextConfiguration);

	/**
	 * Remove the {@linkplain ApplicationContext application context} for the
	 * supplied {@link MergedContextConfiguration} from the {@code ContextCache}
	 * and {@linkplain ConfigurableApplicationContext#close() close} it if it is
	 * an instance of {@link ConfigurableApplicationContext}.
	 * <p>The semantics of the supplied {@code HierarchyMode} must be honored when
	 * removing the context from the cache. See the Javadoc for {@link HierarchyMode}
	 * for details.
	 * <p>Generally speaking, this method should only be called if the state of
	 * a singleton bean has been changed (potentially affecting future interaction
	 * with the context) or if the context needs to be prematurely removed from
	 * the cache.
	 * @param mergedContextConfiguration the merged context configuration for the
	 * application context to close; never {@code null}
	 * @param hierarchyMode the hierarchy mode; may be {@code null} if the context
	 * is not part of a hierarchy
	 * @since 4.1
	 */
	void closeContext(MergedContextConfiguration mergedContextConfiguration, HierarchyMode hierarchyMode);

}
