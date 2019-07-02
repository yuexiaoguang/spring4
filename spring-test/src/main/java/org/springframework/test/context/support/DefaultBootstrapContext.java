package org.springframework.test.context.support;

import org.springframework.core.style.ToStringCreator;
import org.springframework.test.context.BootstrapContext;
import org.springframework.test.context.CacheAwareContextLoaderDelegate;
import org.springframework.util.Assert;

/**
 * {@link BootstrapContext}接口的默认实现.
 */
public class DefaultBootstrapContext implements BootstrapContext {

	private final Class<?> testClass;
	private final CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate;


	/**
	 * @param testClass 此引导上下文的测试类; never {@code null}
	 * @param cacheAwareContextLoaderDelegate 用于与{@code ContextCache}进行透明交互的上下文加载器委托; never {@code null}
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

	@Override
	public String toString() {
		return new ToStringCreator(this)//
		.append("testClass", testClass.getName())//
		.append("cacheAwareContextLoaderDelegate", cacheAwareContextLoaderDelegate.getClass().getName())//
		.toString();
	}

}
