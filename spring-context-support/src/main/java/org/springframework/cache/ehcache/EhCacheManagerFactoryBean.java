package org.springframework.cache.ehcache;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.ConfigurationFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;

/**
 * {@link FactoryBean}公开从指定的配置位置配置的EhCache {@link net.sf.ehcache.CacheManager}实例 (独立或共享).
 *
 * <p>如果未指定配置位置, 则将从类路径根目录中的"ehcache.xml"配置CacheManager
 * (也就是说, 默认的EhCache初始化 - 如EhCache文档中所定义 - 将适用).
 *
 * <p>使用EhCacheFactoryBean时, 建议设置单独的EhCacheManagerFactoryBean,
 * 因为它提供了一个 (默认情况下) 独立的CacheManager实例, 并且关心CacheManager的正确关闭.
 * 从非默认配置位置加载EhCache配置所必需的EhCacheManagerFactoryBean.
 *
 * <p>Note: 从Spring 4.1开始, Spring的EhCache支持需要EhCache 2.5或更高版本.
 */
public class EhCacheManagerFactoryBean implements FactoryBean<CacheManager>, InitializingBean, DisposableBean {

	protected final Log logger = LogFactory.getLog(getClass());

	private Resource configLocation;

	private String cacheManagerName;

	private boolean acceptExisting = false;

	private boolean shared = false;

	private CacheManager cacheManager;

	private boolean locallyManaged = true;


	/**
	 * 设置EhCache配置文件的位置. 通常是"/WEB-INF/ehcache.xml".
	 * <p>默认为类路径根目录中的"ehcache.xml", 如果未找到, 则为EhCache jar中的"ehcache-failsafe.xml"(默认EhCache初始化).
	 */
	public void setConfigLocation(Resource configLocation) {
		this.configLocation = configLocation;
	}

	/**
	 * 设置EhCache CacheManager的名称 (如果需要特定名称).
	 */
	public void setCacheManagerName(String cacheManagerName) {
		this.cacheManagerName = cacheManagerName;
	}

	/**
	 * 设置是否为此EhCacheManagerFactoryBean设置接受现有的同名EhCache CacheManager. 默认"false".
	 * <p>通常与{@link #setCacheManagerName "cacheManagerName"}结合使用, 但如果没有指定, 则只使用默认的CacheManager名称.
	 * 在同一个ClassLoader空间中对相同CacheManager名称(或相同默认值) 的所有引用将共享指定的CacheManager.
	 */
	public void setAcceptExisting(boolean acceptExisting) {
		this.acceptExisting = acceptExisting;
	}

	/**
	 * 设置EhCache CacheManager应该共享 (作为ClassLoader级别的单例) 或独立 (通常是应用程序中的本地).
	 * 默认 "false", 创建一个独立的本地实例.
	 * <p><b>NOTE:</b> 此功能允许与同一ClassLoader空间中调用<code>CacheManager.create()</code>的任何代码
	 * 共享此EhCacheManagerFactoryBean的CacheManager, 无需就特定的CacheManager名称达成一致.
	 * 但是, 它只支持涉及的单个EhCacheManagerFactoryBean, 它将控制底层CacheManager的生命周期 (特别是其关闭).
	 * <p>如果两者都设置, 则此标志将覆盖{@link #setAcceptExisting "acceptExisting"}, 因为它表示'更强'的共享模式.
	 */
	public void setShared(boolean shared) {
		this.shared = shared;
	}


	@Override
	public void afterPropertiesSet() throws CacheException {
		if (logger.isInfoEnabled()) {
			logger.info("Initializing EhCache CacheManager" +
					(this.cacheManagerName != null ? " '" + this.cacheManagerName + "'" : ""));
		}

		Configuration configuration = (this.configLocation != null ?
				EhCacheManagerUtils.parseConfiguration(this.configLocation) : ConfigurationFactory.parseConfiguration());
		if (this.cacheManagerName != null) {
			configuration.setName(this.cacheManagerName);
		}

		if (this.shared) {
			// 老派EhCache单例共享...
			// 无法确定是否实际创建了新的CacheManager, 或者只是收到了现有的单例引用.
			this.cacheManager = CacheManager.create(configuration);
		}
		else if (this.acceptExisting) {
			// EhCache 2.5+: 重用现有的同名CacheManager.
			// 基本上与CacheManager.getInstance(String)中的代码相同, 只是存储是否正在处理现有实例.
			synchronized (CacheManager.class) {
				this.cacheManager = CacheManager.getCacheManager(this.cacheManagerName);
				if (this.cacheManager == null) {
					this.cacheManager = new CacheManager(configuration);
				}
				else {
					this.locallyManaged = false;
				}
			}
		}
		else {
			// 如果已存在同名的CacheManager, 则抛出异常...
			this.cacheManager = new CacheManager(configuration);
		}
	}


	@Override
	public CacheManager getObject() {
		return this.cacheManager;
	}

	@Override
	public Class<? extends CacheManager> getObjectType() {
		return (this.cacheManager != null ? this.cacheManager.getClass() : CacheManager.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}


	@Override
	public void destroy() {
		if (this.locallyManaged) {
			if (logger.isInfoEnabled()) {
				logger.info("Shutting down EhCache CacheManager" +
						(this.cacheManagerName != null ? " '" + this.cacheManagerName + "'" : ""));
			}
			this.cacheManager.shutdown();
		}
	}

}
