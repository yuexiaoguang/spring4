package org.springframework.cache.ehcache;

import java.io.IOException;
import java.io.InputStream;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.ConfigurationFactory;

import org.springframework.core.io.Resource;

/**
 * EhCache 2.5+ {@link CacheManager}设置的构建器方法, 从Spring提供的资源提供简单的编程引导.
 * 这主要用于Spring配置类中的{@code @Bean}方法.
 *
 * <p>这些方法是自定义{@link CacheManager}设置代码的简单替代方法.
 * 出于任何高级目的, 考虑使用{@link #parseConfiguration}, 自定义配置对象,
 * 然后调用{@link CacheManager#CacheManager(Configuration)}构造函数.
 */
public abstract class EhCacheManagerUtils {

	/**
	 * 从默认配置构建EhCache {@link CacheManager}.
	 * <p>CacheManager将从类路径根目录中的 "ehcache.xml"进行配置
	 * (也就是默认的EhCache初始化 - 如EhCache文档中所定义 - 将适用).
	 * 如果找不到配置文件, 则将使用故障安全回退配置.
	 * 
	 * @return 新的EhCache CacheManager
	 * @throws CacheException 配置解析失败
	 */
	public static CacheManager buildCacheManager() throws CacheException {
		return new CacheManager(ConfigurationFactory.parseConfiguration());
	}

	/**
	 * 从默认配置构建EhCache {@link CacheManager}.
	 * <p>CacheManager将从类路径根目录中的 "ehcache.xml"进行配置
	 * (也就是默认的EhCache初始化 - 如EhCache文档中所定义 - 将适用).
	 * 如果找不到配置文件, 则将使用故障安全回退配置.
	 * 
	 * @param name 所需的缓存管理器名称
	 * 
	 * @return 新的EhCache CacheManager
	 * @throws CacheException 配置解析失败
	 */
	public static CacheManager buildCacheManager(String name) throws CacheException {
		Configuration configuration = ConfigurationFactory.parseConfiguration();
		configuration.setName(name);
		return new CacheManager(configuration);
	}

	/**
	 * 从给定的配置资源构建EhCache {@link CacheManager}.
	 * 
	 * @param configLocation 配置文件的位置 (作为Spring资源)
	 * 
	 * @return 新的EhCache CacheManager
	 * @throws CacheException 配置解析失败
	 */
	public static CacheManager buildCacheManager(Resource configLocation) throws CacheException {
		return new CacheManager(parseConfiguration(configLocation));
	}

	/**
	 * 从给定的配置资源构建EhCache {@link CacheManager}.
	 * 
	 * @param name 所需的缓存管理器名称
	 * @param configLocation 配置文件的位置 (作为Spring资源)
	 * 
	 * @return 新的EhCache CacheManager
	 * @throws CacheException 配置解析失败
	 */
	public static CacheManager buildCacheManager(String name, Resource configLocation) throws CacheException {
		Configuration configuration = parseConfiguration(configLocation);
		configuration.setName(name);
		return new CacheManager(configuration);
	}

	/**
	 * 从给定资源中解析EhCache配置, 以便进一步使用自定义{@link CacheManager}创建.
	 * 
	 * @param configLocation 配置文件的位置 (作为Spring资源)
	 * 
	 * @return EhCache Configuration句柄
	 * @throws CacheException 配置解析失败
	 */
	public static Configuration parseConfiguration(Resource configLocation) throws CacheException {
		InputStream is = null;
		try {
			is = configLocation.getInputStream();
			return ConfigurationFactory.parseConfiguration(is);
		}
		catch (IOException ex) {
			throw new CacheException("Failed to parse EhCache configuration resource", ex);
		}
		finally {
			if (is != null) {
				try {
					is.close();
				}
				catch (IOException ex) {
					// ignore
				}
			}
		}
	}
}
