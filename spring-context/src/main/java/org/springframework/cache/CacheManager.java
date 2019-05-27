package org.springframework.cache;

import java.util.Collection;

/**
 * Spring的中央缓存管理器SPI.
 * 允许检索命名的{@link Cache}区域.
 */
public interface CacheManager {

	/**
	 * 返回与给定名称关联的缓存.
	 * 
	 * @param name 缓存标识符 (不能是 {@code null})
	 * 
	 * @return 关联的缓存, 或{@code null}
	 */
	Cache getCache(String name);

	/**
	 * 返回此管理器已知的缓存名称的集合.
	 * 
	 * @return 缓存管理器已知的所有缓存的名称
	 */
	Collection<String> getCacheNames();

}
