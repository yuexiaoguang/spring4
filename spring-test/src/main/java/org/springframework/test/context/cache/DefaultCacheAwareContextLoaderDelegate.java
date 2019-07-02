package org.springframework.test.context.cache;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.context.CacheAwareContextLoaderDelegate;
import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.SmartContextLoader;
import org.springframework.util.Assert;

/**
 * {@link CacheAwareContextLoaderDelegate}接口的默认实现.
 *
 * <p>要使用静态{@code DefaultContextCache}, 调用{@link #DefaultCacheAwareContextLoaderDelegate()}构造函数;
 * 否则, 调用{@link #DefaultCacheAwareContextLoaderDelegate(ContextCache)}并提供自定义的{@link ContextCache}实现.
 */
public class DefaultCacheAwareContextLoaderDelegate implements CacheAwareContextLoaderDelegate {

	private static final Log logger = LogFactory.getLog(DefaultCacheAwareContextLoaderDelegate.class);

	/**
	 * Spring应用程序上下文的默认静态缓存.
	 */
	static final ContextCache defaultContextCache = new DefaultContextCache();

	private final ContextCache contextCache;


	/**
	 * <p>此默认缓存是静态的, 因此可以缓存每个上下文,
	 * 并将其重用于在同一JVM进程中声明相同唯一上下文配置的所有后续测试.
	 */
	public DefaultCacheAwareContextLoaderDelegate() {
		this(defaultContextCache);
	}

	public DefaultCacheAwareContextLoaderDelegate(ContextCache contextCache) {
		Assert.notNull(contextCache, "ContextCache must not be null");
		this.contextCache = contextCache;
	}

	/**
	 * 获取此上下文加载器委托使用的{@link ContextCache}.
	 */
	protected ContextCache getContextCache() {
		return this.contextCache;
	}

	/**
	 * 为所提供的合并上下文配置加载{@code ApplicationContext}.
	 * <p>支持{@link SmartContextLoader} 和 {@link ContextLoader} SPI.
	 * 
	 * @throws Exception 如果加载应用程序上下文时发生错误
	 */
	protected ApplicationContext loadContextInternal(MergedContextConfiguration mergedContextConfiguration)
			throws Exception {

		ContextLoader contextLoader = mergedContextConfiguration.getContextLoader();
		Assert.notNull(contextLoader, "Cannot load an ApplicationContext with a NULL 'contextLoader'. " +
				"Consider annotating your test class with @ContextConfiguration or @ContextHierarchy.");

		ApplicationContext applicationContext;

		if (contextLoader instanceof SmartContextLoader) {
			SmartContextLoader smartContextLoader = (SmartContextLoader) contextLoader;
			applicationContext = smartContextLoader.loadContext(mergedContextConfiguration);
		}
		else {
			String[] locations = mergedContextConfiguration.getLocations();
			Assert.notNull(locations, "Cannot load an ApplicationContext with a NULL 'locations' array. " +
					"Consider annotating your test class with @ContextConfiguration or @ContextHierarchy.");
			applicationContext = contextLoader.loadContext(locations);
		}

		return applicationContext;
	}

	@Override
	public ApplicationContext loadContext(MergedContextConfiguration mergedContextConfiguration) {
		synchronized (this.contextCache) {
			ApplicationContext context = this.contextCache.get(mergedContextConfiguration);
			if (context == null) {
				try {
					context = loadContextInternal(mergedContextConfiguration);
					if (logger.isDebugEnabled()) {
						logger.debug(String.format("Storing ApplicationContext in cache under key [%s]",
								mergedContextConfiguration));
					}
					this.contextCache.put(mergedContextConfiguration, context);
				}
				catch (Exception ex) {
					throw new IllegalStateException("Failed to load ApplicationContext", ex);
				}
			}
			else {
				if (logger.isDebugEnabled()) {
					logger.debug(String.format("Retrieved ApplicationContext from cache with key [%s]",
							mergedContextConfiguration));
				}
			}

			this.contextCache.logStatistics();

			return context;
		}
	}

	@Override
	public void closeContext(MergedContextConfiguration mergedContextConfiguration, HierarchyMode hierarchyMode) {
		synchronized (this.contextCache) {
			this.contextCache.remove(mergedContextConfiguration, hierarchyMode);
		}
	}

}
