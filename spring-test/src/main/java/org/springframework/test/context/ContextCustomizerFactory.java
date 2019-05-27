package org.springframework.test.context;

import java.util.List;

/**
 * Factory for creating {@link ContextCustomizer ContextCustomizers}.
 *
 * <p>Factories are invoked after {@link ContextLoader ContextLoaders} have
 * processed context configuration attributes but before the
 * {@link MergedContextConfiguration} is created.
 *
 * <p>By default, the Spring TestContext Framework will use the
 * {@link org.springframework.core.io.support.SpringFactoriesLoader SpringFactoriesLoader}
 * mechanism for loading factories configured in all {@code META-INF/spring.factories}
 * files on the classpath.
 */
public interface ContextCustomizerFactory {

	/**
	 * Create a {@link ContextCustomizer} that should be used to customize a
	 * {@link org.springframework.context.ConfigurableApplicationContext ConfigurableApplicationContext}
	 * before it is refreshed.
	 * @param testClass the test class
	 * @param configAttributes the list of context configuration attributes for
	 * the test class, ordered <em>bottom-up</em> (i.e., as if we were traversing
	 * up the class hierarchy); never {@code null} or empty
	 * @return a {@link ContextCustomizer} or {@code null} if no customizer should
	 * be used
	 */
	ContextCustomizer createContextCustomizer(Class<?> testClass, List<ContextConfigurationAttributes> configAttributes);

}
