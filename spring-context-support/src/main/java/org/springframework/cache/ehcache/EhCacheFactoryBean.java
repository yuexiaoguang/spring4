package org.springframework.cache.ehcache;

import java.lang.reflect.Method;
import java.util.Set;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.bootstrap.BootstrapCacheLoader;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.constructs.blocking.BlockingCache;
import net.sf.ehcache.constructs.blocking.CacheEntryFactory;
import net.sf.ehcache.constructs.blocking.SelfPopulatingCache;
import net.sf.ehcache.constructs.blocking.UpdatingCacheEntryFactory;
import net.sf.ehcache.constructs.blocking.UpdatingSelfPopulatingCache;
import net.sf.ehcache.event.CacheEventListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * {@link FactoryBean}创建一个命名的EhCache {@link net.sf.ehcache.Cache}的实例
 * (或实现{@link net.sf.ehcache.Ehcache}接口的装饰器),
 * 表示EhCache {@link net.sf.ehcache.CacheManager}中的缓存区域.
 *
 * <p>如果未在缓存配置描述符中配置指定的命名缓存,
 * 这个FactoryBean将使用提供的名称和指定的缓存属性构造一个Cache实例, 并将其添加到CacheManager中以便以后检索.
 * 如果在配置时未设置某些或所有属性, 则此FactoryBean将使用默认值.
 *
 * <p>Note: 如果找到了命名的Cache实例, 则将忽略这些属性, 并从CacheManager中检索Cache实例.
 *
 * <p>Note: As of Spring 4.1, Spring's EhCache support requires EhCache 2.5 or higher.
 */
public class EhCacheFactoryBean extends CacheConfiguration implements FactoryBean<Ehcache>, BeanNameAware, InitializingBean {

	// EhCache's setStatisticsEnabled(boolean) available? Not anymore as of EhCache 2.7...
	private static final Method setStatisticsEnabledMethod =
			ClassUtils.getMethodIfAvailable(Ehcache.class, "setStatisticsEnabled", boolean.class);

	// EhCache's setSampledStatisticsEnabled(boolean) available? Not anymore as of EhCache 2.7...
	private static final Method setSampledStatisticsEnabledMethod =
			ClassUtils.getMethodIfAvailable(Ehcache.class, "setSampledStatisticsEnabled", boolean.class);


	protected final Log logger = LogFactory.getLog(getClass());

	private CacheManager cacheManager;

	private boolean blocking = false;

	private CacheEntryFactory cacheEntryFactory;

	private BootstrapCacheLoader bootstrapCacheLoader;

	private Set<CacheEventListener> cacheEventListeners;

	private boolean statisticsEnabled = false;

	private boolean sampledStatisticsEnabled = false;

	private boolean disabled = false;

	private String beanName;

	private Ehcache cache;


	@SuppressWarnings("deprecation")
	public EhCacheFactoryBean() {
		setMaxEntriesLocalHeap(10000);
		setMaxElementsOnDisk(10000000);
		setTimeToLiveSeconds(120);
		setTimeToIdleSeconds(120);
	}


	/**
	 * 设置CacheManager, 从中检索命名的Cache实例.
	 * 默认情况下, 将调用{@code CacheManager.getInstance()}.
	 * <p>请注意, 特别是对于持久性缓存, 建议正确处理CacheManager的关闭:
	 * 设置一个单独的EhCacheManagerFactoryBean, 并传递对此bean属性的引用.
	 * <p>从非默认配置位置加载EhCache配置, 也需要单独的EhCacheManagerFactoryBean.
	 */
	public void setCacheManager(CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}

	/**
	 * 设置要检索或创建的缓存实例的名称.
	 * 默认是此EhCacheFactoryBean的bean名称.
	 */
	public void setCacheName(String cacheName) {
		setName(cacheName);
	}

	public void setTimeToLive(int timeToLive) {
		setTimeToLiveSeconds(timeToLive);
	}

	public void setTimeToIdle(int timeToIdle) {
		setTimeToIdleSeconds(timeToIdle);
	}

	public void setDiskSpoolBufferSize(int diskSpoolBufferSize) {
		setDiskSpoolBufferSizeMB(diskSpoolBufferSize);
	}

	/**
	 * 设置是否使用阻塞缓存, 以便在创建请求的元素之前阻塞读取.
	 * <p>如果打算构建一个自填充的阻塞缓存, 请考虑指定 {@link #setCacheEntryFactory CacheEntryFactory}.
	 */
	public void setBlocking(boolean blocking) {
		this.blocking = blocking;
	}

	/**
	 * 设置用于自填充缓存的 EhCache {@link net.sf.ehcache.constructs.blocking.CacheEntryFactory}.
	 * 如果指定了这样的工厂, 缓存将用EhCache的{@link net.sf.ehcache.constructs.blocking.SelfPopulatingCache}进行修饰.
	 * <p>指定的工厂可以是{@link net.sf.ehcache.constructs.blocking.UpdatingCacheEntryFactory}类型,
	 * 这将导致使用{@link net.sf.ehcache.constructs.blocking.UpdatingSelfPopulatingCache}.
	 * <p>Note: 任何此类自填充缓存都自动为阻塞缓存.
	 */
	public void setCacheEntryFactory(CacheEntryFactory cacheEntryFactory) {
		this.cacheEntryFactory = cacheEntryFactory;
	}

	/**
	 * 设置此缓存的EhCache {@link net.sf.ehcache.bootstrap.BootstrapCacheLoader}.
	 */
	public void setBootstrapCacheLoader(BootstrapCacheLoader bootstrapCacheLoader) {
		this.bootstrapCacheLoader = bootstrapCacheLoader;
	}

	/**
	 * 指定此缓存注册的EhCache {@link net.sf.ehcache.event.CacheEventListener 缓存事件侦听器}.
	 */
	public void setCacheEventListeners(Set<CacheEventListener> cacheEventListeners) {
		this.cacheEventListeners = cacheEventListeners;
	}

	/**
	 * 设置是否在此缓存上启用EhCache统计信息.
	 * <p>Note: 从EhCache 2.7开始, 默认情况下会启用统计信息, 并且无法关闭统计信息.
	 * 因此, 此setter在这种情况下没有效果.
	 */
	public void setStatisticsEnabled(boolean statisticsEnabled) {
		this.statisticsEnabled = statisticsEnabled;
	}

	/**
	 * 设置是否在此缓存上启用EhCache的采样统计信息.
	 * <p>Note: 从EhCache 2.7开始, 默认情况下会启用统计信息, 并且无法关闭统计信息.
	 * 因此, 此setter在这种情况下没有效果.
	 */
	public void setSampledStatisticsEnabled(boolean sampledStatisticsEnabled) {
		this.sampledStatisticsEnabled = sampledStatisticsEnabled;
	}

	/**
	 * 设置是否应将此缓存标记为已禁用.
	 */
	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}

	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}


	@Override
	public void afterPropertiesSet() throws CacheException {
		// 如果没有给出缓存名称, 请使用bean名称作为缓存名称.
		String cacheName = getName();
		if (cacheName == null) {
			cacheName = this.beanName;
			setName(cacheName);
		}

		// 如果没有给出CacheManager, 则获取默认值.
		if (this.cacheManager == null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Using default EhCache CacheManager for cache region '" + cacheName + "'");
			}
			this.cacheManager = CacheManager.getInstance();
		}

		synchronized (this.cacheManager) {
			// 获取缓存区域: 如果不存在具有给定名称的区域, 请立即创建一个.
			Ehcache rawCache;
			boolean cacheExists = this.cacheManager.cacheExists(cacheName);

			if (cacheExists) {
				if (logger.isDebugEnabled()) {
					logger.debug("Using existing EhCache cache region '" + cacheName + "'");
				}
				rawCache = this.cacheManager.getEhcache(cacheName);
			}
			else {
				if (logger.isDebugEnabled()) {
					logger.debug("Creating new EhCache cache region '" + cacheName + "'");
				}
				rawCache = createCache();
				rawCache.setBootstrapCacheLoader(this.bootstrapCacheLoader);
			}

			if (this.cacheEventListeners != null) {
				for (CacheEventListener listener : this.cacheEventListeners) {
					rawCache.getCacheEventNotificationService().registerListener(listener);
				}
			}

			// 需要在监听器注册之后但在setStatisticsEnabled之前发生
			if (!cacheExists) {
				this.cacheManager.addCache(rawCache);
			}

			// 只有在EhCache <2.7时才需要: 从2.7开始, 统计信息默认开启.
			if (this.statisticsEnabled && setStatisticsEnabledMethod != null) {
				ReflectionUtils.invokeMethod(setStatisticsEnabledMethod, rawCache, true);
			}
			if (this.sampledStatisticsEnabled && setSampledStatisticsEnabledMethod != null) {
				ReflectionUtils.invokeMethod(setSampledStatisticsEnabledMethod, rawCache, true);
			}

			if (this.disabled) {
				rawCache.setDisabled(true);
			}

			Ehcache decoratedCache = decorateCache(rawCache);
			if (decoratedCache != rawCache) {
				this.cacheManager.replaceCacheWithDecoratedCache(rawCache, decoratedCache);
			}
			this.cache = decoratedCache;
		}
	}

	/**
	 * 根据此FactoryBean的配置创建原始Cache对象.
	 */
	protected Cache createCache() {
		return new Cache(this);
	}

	/**
	 * 装饰给定的Cache.
	 * 
	 * @param cache 原始Cache对象, 基于此FactoryBean的配置
	 * 
	 * @return 要使用CacheManager注册的(可能已修饰的)缓存对象
	 */
	protected Ehcache decorateCache(Ehcache cache) {
		if (this.cacheEntryFactory != null) {
			if (this.cacheEntryFactory instanceof UpdatingCacheEntryFactory) {
				return new UpdatingSelfPopulatingCache(cache, (UpdatingCacheEntryFactory) this.cacheEntryFactory);
			}
			else {
				return new SelfPopulatingCache(cache, this.cacheEntryFactory);
			}
		}
		if (this.blocking) {
			return new BlockingCache(cache);
		}
		return cache;
	}


	@Override
	public Ehcache getObject() {
		return this.cache;
	}

	/**
	 * 预测将从{@link #getObject()}返回的特定{@code Ehcache}实现,
	 * 基于{@link #createCache()}和{@link #decorateCache(Ehcache)}中的逻辑, 由{@link #afterPropertiesSet()}编排.
	 */
	@Override
	public Class<? extends Ehcache> getObjectType() {
		if (this.cache != null) {
			return this.cache.getClass();
		}
		if (this.cacheEntryFactory != null) {
			if (this.cacheEntryFactory instanceof UpdatingCacheEntryFactory) {
				return UpdatingSelfPopulatingCache.class;
			}
			else {
				return SelfPopulatingCache.class;
			}
		}
		if (this.blocking) {
			return BlockingCache.class;
		}
		return Cache.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
