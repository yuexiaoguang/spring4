package org.springframework.test.context;

import java.lang.reflect.Constructor;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.util.ClassUtils;

/**
 * {@code BootstrapUtils}是一组工具方法, 用于协助引导<em>Spring TestContext Framework</em>.
 */
abstract class BootstrapUtils {

	private static final String DEFAULT_BOOTSTRAP_CONTEXT_CLASS_NAME =
			"org.springframework.test.context.support.DefaultBootstrapContext";

	private static final String DEFAULT_CACHE_AWARE_CONTEXT_LOADER_DELEGATE_CLASS_NAME =
			"org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate";

	private static final String DEFAULT_TEST_CONTEXT_BOOTSTRAPPER_CLASS_NAME =
			"org.springframework.test.context.support.DefaultTestContextBootstrapper";

	private static final String DEFAULT_WEB_TEST_CONTEXT_BOOTSTRAPPER_CLASS_NAME =
			"org.springframework.test.context.web.WebTestContextBootstrapper";

	private static final String WEB_APP_CONFIGURATION_ANNOTATION_CLASS_NAME =
			"org.springframework.test.context.web.WebAppConfiguration";

	private static final Log logger = LogFactory.getLog(BootstrapUtils.class);


	/**
	 * 为指定的{@linkplain Class 测试类}创建{@code BootstrapContext}.
	 * <p>使用反射创建使用{@link org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate}的
	 * {@link org.springframework.test.context.support.DefaultBootstrapContext}.
	 * 
	 * @param testClass 应为其创建引导上下文的测试类
	 * 
	 * @return 新{@code BootstrapContext}; never {@code null}
	 */
	@SuppressWarnings("unchecked")
	static BootstrapContext createBootstrapContext(Class<?> testClass) {
		CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate = createCacheAwareContextLoaderDelegate();
		Class<? extends BootstrapContext> clazz = null;
		try {
			clazz = (Class<? extends BootstrapContext>) ClassUtils.forName(
					DEFAULT_BOOTSTRAP_CONTEXT_CLASS_NAME, BootstrapUtils.class.getClassLoader());
			Constructor<? extends BootstrapContext> constructor = clazz.getConstructor(
					Class.class, CacheAwareContextLoaderDelegate.class);
			if (logger.isDebugEnabled()) {
				logger.debug(String.format("Instantiating BootstrapContext using constructor [%s]", constructor));
			}
			return BeanUtils.instantiateClass(constructor, testClass, cacheAwareContextLoaderDelegate);
		}
		catch (Throwable ex) {
			throw new IllegalStateException("Could not load BootstrapContext [" + clazz + "]", ex);
		}
	}

	@SuppressWarnings("unchecked")
	private static CacheAwareContextLoaderDelegate createCacheAwareContextLoaderDelegate() {
		Class<? extends CacheAwareContextLoaderDelegate> clazz = null;
		try {
			clazz = (Class<? extends CacheAwareContextLoaderDelegate>) ClassUtils.forName(
				DEFAULT_CACHE_AWARE_CONTEXT_LOADER_DELEGATE_CLASS_NAME, BootstrapUtils.class.getClassLoader());

			if (logger.isDebugEnabled()) {
				logger.debug(String.format("Instantiating CacheAwareContextLoaderDelegate from class [%s]",
					clazz.getName()));
			}
			return BeanUtils.instantiateClass(clazz, CacheAwareContextLoaderDelegate.class);
		}
		catch (Throwable ex) {
			throw new IllegalStateException("Could not load CacheAwareContextLoaderDelegate [" + clazz + "]", ex);
		}
	}

	/**
	 * 在提供的{@link BootstrapContext}中解析测试类的{@link TestContextBootstrapper}类型,
	 * 对其进行实例化, 并为其提供对{@link BootstrapContext}的引用.
	 * <p>如果测试类上存在{@link BootstrapWith @BootstrapWith}注解, 可以直接使用或作为元注解,
	 * 那么它的{@link BootstrapWith#value value}将用作引导程序类型.
	 * 否则, 将使用
	 * {@link org.springframework.test.context.support.DefaultTestContextBootstrapper DefaultTestContextBootstrapper}
	 * 或{@link org.springframework.test.context.web.WebTestContextBootstrapper WebTestContextBootstrapper},
	 * 具体取决于
	 * {@link org.springframework.test.context.web.WebAppConfiguration @WebAppConfiguration}的存在.
	 * 
	 * @param bootstrapContext 要使用的引导上下文
	 * 
	 * @return 完全配置的{@code TestContextBootstrapper}
	 */
	static TestContextBootstrapper resolveTestContextBootstrapper(BootstrapContext bootstrapContext) {
		Class<?> testClass = bootstrapContext.getTestClass();

		Class<?> clazz = null;
		try {
			clazz = resolveExplicitTestContextBootstrapper(testClass);
			if (clazz == null) {
				clazz = resolveDefaultTestContextBootstrapper(testClass);
			}
			if (logger.isDebugEnabled()) {
				logger.debug(String.format("Instantiating TestContextBootstrapper for test class [%s] from class [%s]",
						testClass.getName(), clazz.getName()));
			}
			TestContextBootstrapper testContextBootstrapper =
					BeanUtils.instantiateClass(clazz, TestContextBootstrapper.class);
			testContextBootstrapper.setBootstrapContext(bootstrapContext);
			return testContextBootstrapper;
		}
		catch (IllegalStateException ex) {
			throw ex;
		}
		catch (Throwable ex) {
			throw new IllegalStateException("Could not load TestContextBootstrapper [" + clazz +
					"]. Specify @BootstrapWith's 'value' attribute or make the default bootstrapper class available.",
					ex);
		}
	}

	private static Class<?> resolveExplicitTestContextBootstrapper(Class<?> testClass) {
		Set<BootstrapWith> annotations = AnnotatedElementUtils.findAllMergedAnnotations(testClass, BootstrapWith.class);
		if (annotations.size() < 1) {
			return null;
		}
		if (annotations.size() > 1) {
			throw new IllegalStateException(String.format(
				"Configuration error: found multiple declarations of @BootstrapWith for test class [%s]: %s",
				testClass.getName(), annotations));
		}
		return annotations.iterator().next().value();
	}

	private static Class<?> resolveDefaultTestContextBootstrapper(Class<?> testClass) throws Exception {
		ClassLoader classLoader = BootstrapUtils.class.getClassLoader();
		AnnotationAttributes attributes = AnnotatedElementUtils.findMergedAnnotationAttributes(testClass,
			WEB_APP_CONFIGURATION_ANNOTATION_CLASS_NAME, false, false);
		if (attributes != null) {
			return ClassUtils.forName(DEFAULT_WEB_TEST_CONTEXT_BOOTSTRAPPER_CLASS_NAME, classLoader);
		}
		return ClassUtils.forName(DEFAULT_TEST_CONTEXT_BOOTSTRAPPER_CLASS_NAME, classLoader);
	}

}
