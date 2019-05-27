package org.springframework.cache.interceptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.springframework.cache.CacheManager;

/**
 * 强制解析到一个给定{@link CacheManager}的可配置名称集合.
 */
public class NamedCacheResolver extends AbstractCacheResolver {

	private Collection<String> cacheNames;


	public NamedCacheResolver() {
	}

	public NamedCacheResolver(CacheManager cacheManager, String... cacheNames) {
		super(cacheManager);
		this.cacheNames = new ArrayList<String>(Arrays.asList(cacheNames));
	}


	/**
	 * 设置此解析程序应使用的缓存名称.
	 */
	public void setCacheNames(Collection<String> cacheNames) {
		this.cacheNames = cacheNames;
	}

	@Override
	protected Collection<String> getCacheNames(CacheOperationInvocationContext<?> context) {
		return this.cacheNames;
	}

}
