package org.springframework.test.context.web;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.TestContextBootstrapper;
import org.springframework.test.context.support.DefaultTestContextBootstrapper;

/**
 * Web-specific implementation of the {@link TestContextBootstrapper} SPI.
 *
 * <ul>
 * <li>Uses {@link WebDelegatingSmartContextLoader} as the default {@link ContextLoader}
 * if the test class is annotated with {@link WebAppConfiguration @WebAppConfiguration}
 * and otherwise delegates to the superclass.
 * <li>Builds a {@link WebMergedContextConfiguration} if the test class is annotated
 * with {@link WebAppConfiguration @WebAppConfiguration}.
 * </ul>
 */
public class WebTestContextBootstrapper extends DefaultTestContextBootstrapper {

	/**
	 * Returns {@link WebDelegatingSmartContextLoader} if the supplied class is
	 * annotated with {@link WebAppConfiguration @WebAppConfiguration} and
	 * otherwise delegates to the superclass.
	 */
	@Override
	protected Class<? extends ContextLoader> getDefaultContextLoaderClass(Class<?> testClass) {
		if (AnnotatedElementUtils.findMergedAnnotation(testClass, WebAppConfiguration.class) != null) {
			return WebDelegatingSmartContextLoader.class;
		}
		else {
			return super.getDefaultContextLoaderClass(testClass);
		}
	}

	/**
	 * Returns a {@link WebMergedContextConfiguration} if the test class in the
	 * supplied {@code MergedContextConfiguration} is annotated with
	 * {@link WebAppConfiguration @WebAppConfiguration} and otherwise returns
	 * the supplied instance unmodified.
	 */
	@Override
	protected MergedContextConfiguration processMergedContextConfiguration(MergedContextConfiguration mergedConfig) {
		WebAppConfiguration webAppConfiguration =
				AnnotatedElementUtils.findMergedAnnotation(mergedConfig.getTestClass(), WebAppConfiguration.class);
		if (webAppConfiguration != null) {
			return new WebMergedContextConfiguration(mergedConfig, webAppConfiguration.value());
		}
		else {
			return mergedConfig;
		}
	}

}
