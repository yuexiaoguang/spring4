package org.springframework.web.servlet.resource;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.web.servlet.resource.ResourceTransformer},
 * 它检查{@link org.springframework.cache.Cache}以查看先前转换的资源是否存在于缓存中, 如果找到就返回它,
 * 否则委托给解析器链并将结果保存在缓存中.
 */
public class CachingResourceTransformer implements ResourceTransformer {

	private static final Log logger = LogFactory.getLog(CachingResourceTransformer.class);

	private final Cache cache;


	public CachingResourceTransformer(CacheManager cacheManager, String cacheName) {
		this(cacheManager.getCache(cacheName));
	}

	public CachingResourceTransformer(Cache cache) {
		Assert.notNull(cache, "Cache is required");
		this.cache = cache;
	}


	/**
	 * 返回配置的{@code Cache}.
	 */
	public Cache getCache() {
		return this.cache;
	}


	@Override
	public Resource transform(HttpServletRequest request, Resource resource, ResourceTransformerChain transformerChain)
			throws IOException {

		Resource transformed = this.cache.get(resource, Resource.class);
		if (transformed != null) {
			if (logger.isTraceEnabled()) {
				logger.trace("Found match: " + transformed);
			}
			return transformed;
		}

		transformed = transformerChain.transform(request, resource);

		if (logger.isTraceEnabled()) {
			logger.trace("Putting transformed resource in cache: " + transformed);
		}
		this.cache.put(resource, transformed);

		return transformed;
	}
}
