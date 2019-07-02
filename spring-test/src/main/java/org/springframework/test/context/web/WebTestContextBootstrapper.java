package org.springframework.test.context.web;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.TestContextBootstrapper;
import org.springframework.test.context.support.DefaultTestContextBootstrapper;

/**
 * 特定于Web的{@link TestContextBootstrapper} SPI实现.
 *
 * <ul>
 * <li>如果测试类使用{@link WebAppConfiguration @WebAppConfiguration}注解,
 * 则使用{@link WebDelegatingSmartContextLoader}作为默认的{@link ContextLoader}, 否则委托给超类.
 * <li>如果测试类使用{@link WebAppConfiguration @WebAppConfiguration}注解,
 * 则构建{@link WebMergedContextConfiguration}.
 * </ul>
 */
public class WebTestContextBootstrapper extends DefaultTestContextBootstrapper {

	/**
	 * 如果提供的类使用{@link WebAppConfiguration @WebAppConfiguration}注解,
	 * 则返回{@link WebDelegatingSmartContextLoader}, 否则委托给超类.
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
	 * 如果提供的{@code MergedContextConfiguration}中的测试类使用{@link WebAppConfiguration @WebAppConfiguration}注解,
	 * 则返回{@link WebMergedContextConfiguration}, 否则返回未修改的提供的实例.
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
