package org.springframework.cache.jcache;

import java.net.URI;
import java.util.Properties;
import javax.cache.CacheManager;
import javax.cache.Caching;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * {@link FactoryBean}用于JCache {@link javax.cache.CacheManager},
 * 通过标准JCache {@link javax.cache.Caching}类按名称获取预定义的CacheManager.
 *
 * <p>Note: 从Spring 4.0开始, 此类已针对JCache 1.0进行了更新.
 */
public class JCacheManagerFactoryBean
		implements FactoryBean<CacheManager>, BeanClassLoaderAware, InitializingBean, DisposableBean {

	private URI cacheManagerUri;

	private Properties cacheManagerProperties;

	private ClassLoader beanClassLoader;

	private CacheManager cacheManager;


	/**
	 * 指定所需CacheManager的URI.
	 * 默认{@code null} (i.e. JCache的默认值).
	 */
	public void setCacheManagerUri(URI cacheManagerUri) {
		this.cacheManagerUri = cacheManagerUri;
	}

	/**
	 * 指定要创建的CacheManager的属性.
	 * 默认{@code null} (i.e. 没有要应用的特殊属性).
	 */
	public void setCacheManagerProperties(Properties cacheManagerProperties) {
		this.cacheManagerProperties = cacheManagerProperties;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	@Override
	public void afterPropertiesSet() {
		this.cacheManager = Caching.getCachingProvider().getCacheManager(
				this.cacheManagerUri, this.beanClassLoader, this.cacheManagerProperties);
	}


	@Override
	public CacheManager getObject() {
		return this.cacheManager;
	}

	@Override
	public Class<?> getObjectType() {
		return (this.cacheManager != null ? this.cacheManager.getClass() : CacheManager.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}


	@Override
	public void destroy() {
		this.cacheManager.close();
	}

}
