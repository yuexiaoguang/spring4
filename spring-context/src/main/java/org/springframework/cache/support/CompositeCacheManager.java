package org.springframework.cache.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

/**
 * 复合{@link CacheManager}实现, 它迭代给定的{@link CacheManager}实例的给定集合.
 *
 * <p>允许{@link NoOpCacheManager}自动添加到列表末尾, 以便在没有后备存储的情况下处理缓存声明.
 * 否则, 任何自定义{@link CacheManager}也可以扮演最后一个委托的角色, 延迟地为任何请求的名称创建缓存区域.
 *
 * <p>Note: 此复合管理器委托的常规CacheManagers需要从 {@link #getCache(String)}返回{@code null},
 * 如果他们不知道指定的缓存名称, 则允许迭代到下一个委托.
 * 但是, 大多数{@link CacheManager}实现都会在请求后回退到命名缓存的延迟创建;
 * 检查具有固定缓存名称的“静态”模式的特定配置详细信息.
 */
public class CompositeCacheManager implements CacheManager, InitializingBean {

	private final List<CacheManager> cacheManagers = new ArrayList<CacheManager>();

	private boolean fallbackToNoOpCache = false;


	/**
	 * 构造一个空的CompositeCacheManager, 通过 {@link #setCacheManagers "cacheManagers"}属性添加委托CacheManager.
	 */
	public CompositeCacheManager() {
	}

	/**
	 * @param cacheManagers 要委托给的CacheManagers
	 */
	public CompositeCacheManager(CacheManager... cacheManagers) {
		setCacheManagers(Arrays.asList(cacheManagers));
	}


	/**
	 * 指定要委派的CacheManagers.
	 */
	public void setCacheManagers(Collection<CacheManager> cacheManagers) {
		this.cacheManagers.addAll(cacheManagers);
	}

	/**
	 * 指示是否应在委托列表的末尾添加{@link NoOpCacheManager}.
	 * 在这种情况下, {@link NoOpCacheManager}将自动处理未由配置的CacheManagers处理的{@code getCache}请求 (因此永远不会返回 {@code null}).
	 */
	public void setFallbackToNoOpCache(boolean fallbackToNoOpCache) {
		this.fallbackToNoOpCache = fallbackToNoOpCache;
	}

	@Override
	public void afterPropertiesSet() {
		if (this.fallbackToNoOpCache) {
			this.cacheManagers.add(new NoOpCacheManager());
		}
	}


	@Override
	public Cache getCache(String name) {
		for (CacheManager cacheManager : this.cacheManagers) {
			Cache cache = cacheManager.getCache(name);
			if (cache != null) {
				return cache;
			}
		}
		return null;
	}

	@Override
	public Collection<String> getCacheNames() {
		Set<String> names = new LinkedHashSet<String>();
		for (CacheManager manager : this.cacheManagers) {
			names.addAll(manager.getCacheNames());
		}
		return Collections.unmodifiableSet(names);
	}

}
