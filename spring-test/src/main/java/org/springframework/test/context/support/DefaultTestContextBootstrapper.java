package org.springframework.test.context.support;

import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.TestContextBootstrapper;

/**
 * {@link TestContextBootstrapper} SPI的默认实现.
 *
 * <p>使用{@link DelegatingSmartContextLoader}作为默认的{@link ContextLoader}.
 */
public class DefaultTestContextBootstrapper extends AbstractTestContextBootstrapper {

	/**
	 * Returns {@link DelegatingSmartContextLoader}.
	 */
	@Override
	protected Class<? extends ContextLoader> getDefaultContextLoaderClass(Class<?> testClass) {
		return DelegatingSmartContextLoader.class;
	}

}
