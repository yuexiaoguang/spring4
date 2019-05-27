package org.springframework.cache.interceptor;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;

import org.springframework.util.Assert;

/**
 * 复合{@link CacheOperationSource}实现, 它迭代{@code CacheOperationSource}实例的给定数组.
 */
@SuppressWarnings("serial")
public class CompositeCacheOperationSource implements CacheOperationSource, Serializable {

	private final CacheOperationSource[] cacheOperationSources;


	/**
	 * @param cacheOperationSources 要合并的CacheOperationSource实例
	 */
	public CompositeCacheOperationSource(CacheOperationSource... cacheOperationSources) {
		Assert.notEmpty(cacheOperationSources, "cacheOperationSources array must not be empty");
		this.cacheOperationSources = cacheOperationSources;
	}

	/**
	 * 返回此{@code CompositeCacheOperationSource}组合的{@code CacheOperationSource}实例.
	 */
	public final CacheOperationSource[] getCacheOperationSources() {
		return this.cacheOperationSources;
	}

	@Override
	public Collection<CacheOperation> getCacheOperations(Method method, Class<?> targetClass) {
		Collection<CacheOperation> ops = null;

		for (CacheOperationSource source : this.cacheOperationSources) {
			Collection<CacheOperation> cacheOperations = source.getCacheOperations(method, targetClass);
			if (cacheOperations != null) {
				if (ops == null) {
					ops = new ArrayList<CacheOperation>();
				}

				ops.addAll(cacheOperations);
			}
		}
		return ops;
	}
}
