package org.springframework.test.context;

/**
 * {@code BootstrapContext}封装了<em>Spring TestContext Framework</em>引导的上下文.
 */
public interface BootstrapContext {

	/**
	 * 获取此引导上下文的{@linkplain Class 测试类}.
	 * 
	 * @return 测试类 (never {@code null})
	 */
	Class<?> getTestClass();

	/**
	 * 获取用于与{@code ContextCache}的透明交互的{@link CacheAwareContextLoaderDelegate}.
	 * 
	 * @return 上下文加载器委托 (never {@code null})
	 */
	CacheAwareContextLoaderDelegate getCacheAwareContextLoaderDelegate();

}
