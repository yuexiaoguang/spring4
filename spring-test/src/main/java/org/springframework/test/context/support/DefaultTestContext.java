package org.springframework.test.context.support;

import java.lang.reflect.Method;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.AttributeAccessorSupport;
import org.springframework.core.style.ToStringCreator;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.context.CacheAwareContextLoaderDelegate;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.util.Assert;

/**
 * {@link TestContext}接口的默认实现.
 */
public class DefaultTestContext extends AttributeAccessorSupport implements TestContext {

	private static final long serialVersionUID = -5827157174866681233L;

	private final CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate;

	private final MergedContextConfiguration mergedContextConfiguration;

	private final Class<?> testClass;

	private Object testInstance;

	private Method testMethod;

	private Throwable testException;


	/**
	 * @param testClass 此测试上下文的测试类; never {@code null}
	 * @param mergedContextConfiguration 此测试上下文的合并应用程序上下文配置; never {@code null}
	 * @param cacheAwareContextLoaderDelegate 用于加载和关闭此测试上下文的应用程序上下文的委托; never {@code null}
	 */
	public DefaultTestContext(Class<?> testClass, MergedContextConfiguration mergedContextConfiguration,
			CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate) {
		Assert.notNull(testClass, "testClass must not be null");
		Assert.notNull(mergedContextConfiguration, "MergedContextConfiguration must not be null");
		Assert.notNull(cacheAwareContextLoaderDelegate, "CacheAwareContextLoaderDelegate must not be null");
		this.testClass = testClass;
		this.mergedContextConfiguration = mergedContextConfiguration;
		this.cacheAwareContextLoaderDelegate = cacheAwareContextLoaderDelegate;
	}

	/**
	 * 获取此测试上下文的{@linkplain ApplicationContext 应用程序上下文}.
	 * <p>默认实现委托给构造{@code TestContext}时提供的{@link CacheAwareContextLoaderDelegate}.
	 * 
	 * @throws IllegalStateException 如果上下文加载器委托返回的上下文不是<em>活动的</em> (i.e., 已关闭).
	 */
	public ApplicationContext getApplicationContext() {
		ApplicationContext context = this.cacheAwareContextLoaderDelegate.loadContext(this.mergedContextConfiguration);
		if (context instanceof ConfigurableApplicationContext) {
			@SuppressWarnings("resource")
			ConfigurableApplicationContext cac = (ConfigurableApplicationContext) context;
			Assert.state(cac.isActive(), "The ApplicationContext loaded for [" + mergedContextConfiguration
					+ "] is not active. Ensure that the context has not been closed programmatically.");
		}
		return context;
	}

	/**
	 * 将与此测试上下文关联的{@linkplain ApplicationContext 应用程序上下文}标记为<em>dirty</em> (i.e., 将其从上下文缓存中删除并关闭它).
	 * <p>默认实现委托给构造{@code TestContext}时提供的{@link CacheAwareContextLoaderDelegate}.
	 */
	public void markApplicationContextDirty(HierarchyMode hierarchyMode) {
		this.cacheAwareContextLoaderDelegate.closeContext(this.mergedContextConfiguration, hierarchyMode);
	}

	public final Class<?> getTestClass() {
		return this.testClass;
	}

	public final Object getTestInstance() {
		return this.testInstance;
	}

	public final Method getTestMethod() {
		return this.testMethod;
	}

	public final Throwable getTestException() {
		return this.testException;
	}

	public void updateState(Object testInstance, Method testMethod, Throwable testException) {
		this.testInstance = testInstance;
		this.testMethod = testMethod;
		this.testException = testException;
	}


	@Override
	public String toString() {
		return new ToStringCreator(this)
				.append("testClass", this.testClass)
				.append("testInstance", this.testInstance)
				.append("testMethod", this.testMethod)
				.append("testException", this.testException)
				.append("mergedContextConfiguration", this.mergedContextConfiguration)
				.toString();
	}

}
