package org.springframework.cache.jcache.interceptor;

import java.util.Collection;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleCacheResolver;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.util.Assert;

/**
 * 默认的{@link JCacheOperationSource}实现, 将默认操作委托给具有合理默认值的可配置服务, 如果不存在.
 */
public class DefaultJCacheOperationSource extends AnnotationJCacheOperationSource
		implements BeanFactoryAware, InitializingBean, SmartInitializingSingleton {

	private CacheManager cacheManager;

	private CacheResolver cacheResolver;

	private CacheResolver exceptionCacheResolver;

	private KeyGenerator keyGenerator = new SimpleKeyGenerator();

	private KeyGenerator adaptedKeyGenerator;

	private BeanFactory beanFactory;


	/**
	 * 设置默认的{@link CacheManager}以用于按名称查找缓存.
	 * 仅在尚未设置{@linkplain CacheResolver 缓存解析器}时才强制使用.
	 */
	public void setCacheManager(CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}

	/**
	 * 返回要使用的指定的缓存管理器.
	 */
	public CacheManager getCacheManager() {
		return this.cacheManager;
	}

	/**
	 * 设置解析常规缓存的{@link CacheResolver}.
	 * 如果未设置, 则将使用使用指定缓存管理器的默认实现.
	 */
	public void setCacheResolver(CacheResolver cacheResolver) {
		this.cacheResolver = cacheResolver;
	}

	/**
	 * 返回要使用的指定的缓存解析器.
	 */
	public CacheResolver getCacheResolver() {
		return this.cacheResolver;
	}

	/**
	 * 设置用于解析异常缓存的{@link CacheResolver}.
	 * 如果未设置, 则将使用使用指定缓存管理器的默认实现.
	 */
	public void setExceptionCacheResolver(CacheResolver exceptionCacheResolver) {
		this.exceptionCacheResolver = exceptionCacheResolver;
	}

	/**
	 * 返回要使用的指定异常缓存解析器.
	 */
	public CacheResolver getExceptionCacheResolver() {
		return this.exceptionCacheResolver;
	}

	/**
	 * 设置默认的{@link KeyGenerator}.
	 * 如果没有设置, 将使用{@link SimpleKeyGenerator}来表达JSR-107 {@link javax.cache.annotation.CacheKey}
	 * 和{@link javax.cache.annotation.CacheValue}.
	 */
	public void setKeyGenerator(KeyGenerator keyGenerator) {
		this.keyGenerator = keyGenerator;
	}

	/**
	 * 返回要使用的指定Key生成器.
	 */
	public KeyGenerator getKeyGenerator() {
		return this.keyGenerator;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}


	@Override
	public void afterPropertiesSet() {
		this.adaptedKeyGenerator = new KeyGeneratorAdapter(this, this.keyGenerator);
	}

	@Override
	public void afterSingletonsInstantiated() {
		// 确保已初始化缓存解析器.
		// 仅当在操作上设置了exceptionCacheName属性时, 才需要异常缓存解析器
		Assert.notNull(getDefaultCacheResolver(), "Cache resolver should have been initialized");
	}


	@Override
	protected <T> T getBean(Class<T> type) {
		try {
			return this.beanFactory.getBean(type);
		}
		catch (NoUniqueBeanDefinitionException ex) {
			throw new IllegalStateException("No unique [" + type.getName() + "] bean found in application context - " +
					"mark one as primary, or declare a more specific implementation type for your cache", ex);
		}
		catch (NoSuchBeanDefinitionException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("No bean of type [" + type.getName() + "] found in application context", ex);
			}
			return BeanUtils.instantiateClass(type);
		}
	}

	protected CacheManager getDefaultCacheManager() {
		if (this.cacheManager == null) {
			try {
				this.cacheManager = this.beanFactory.getBean(CacheManager.class);
			}
			catch (NoUniqueBeanDefinitionException ex) {
				throw new IllegalStateException("No unique bean of type CacheManager found. "+
						"Mark one as primary or declare a specific CacheManager to use.");
			}
			catch (NoSuchBeanDefinitionException ex) {
				throw new IllegalStateException("No bean of type CacheManager found. Register a CacheManager "+
						"bean or remove the @EnableCaching annotation from your configuration.");
			}
		}
		return this.cacheManager;
	}

	@Override
	protected CacheResolver getDefaultCacheResolver() {
		if (this.cacheResolver == null) {
			this.cacheResolver = new SimpleCacheResolver(getDefaultCacheManager());
		}
		return this.cacheResolver;
	}

	@Override
	protected CacheResolver getDefaultExceptionCacheResolver() {
		if (this.exceptionCacheResolver == null) {
			this.exceptionCacheResolver = new LazyCacheResolver();
		}
		return this.exceptionCacheResolver;
	}

	@Override
	protected KeyGenerator getDefaultKeyGenerator() {
		return this.adaptedKeyGenerator;
	}


	/**
	 * 只有在需要处理异常时, 才解析默认的异常缓存解析器.
	 * <p>非JSR-107设置需要{@link CacheManager}或{@link CacheResolver}.
	 * 如果只指定了后者, 则无法从自定义{@code CacheResolver}实现中提取默认异常{@code CacheResolver}, 因此必须在{@code CacheManager}上回退.
	 * <p>这给出了这种奇怪的情况, 即完全有效的配置会突然中断, 因为启用了JCache支持.
	 * 为避免这种情况, 尽可能晚地解析默认异常{@code CacheResolver}, 以避免在其他情况下出现这种硬性要求.
	 */
	class LazyCacheResolver implements CacheResolver {

		private CacheResolver cacheResolver;

		@Override
		public Collection<? extends Cache> resolveCaches(CacheOperationInvocationContext<?> context) {
			if (this.cacheResolver == null) {
				this.cacheResolver = new SimpleExceptionCacheResolver(getDefaultCacheManager());
			}
			return this.cacheResolver.resolveCaches(context);
		}
	}

}
