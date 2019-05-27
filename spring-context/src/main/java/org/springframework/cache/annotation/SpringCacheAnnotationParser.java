package org.springframework.cache.annotation;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;

import org.springframework.cache.interceptor.CacheEvictOperation;
import org.springframework.cache.interceptor.CacheOperation;
import org.springframework.cache.interceptor.CachePutOperation;
import org.springframework.cache.interceptor.CacheableOperation;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * 解析Spring的{@link Caching}, {@link Cacheable}, {@link CacheEvict}, {@link CachePut}注解的策略实现.
 */
@SuppressWarnings("serial")
public class SpringCacheAnnotationParser implements CacheAnnotationParser, Serializable {

	@Override
	public Collection<CacheOperation> parseCacheAnnotations(Class<?> type) {
		DefaultCacheConfig defaultConfig = getDefaultCacheConfig(type);
		return parseCacheAnnotations(defaultConfig, type);
	}

	@Override
	public Collection<CacheOperation> parseCacheAnnotations(Method method) {
		DefaultCacheConfig defaultConfig = getDefaultCacheConfig(method.getDeclaringClass());
		return parseCacheAnnotations(defaultConfig, method);
	}

	protected Collection<CacheOperation> parseCacheAnnotations(DefaultCacheConfig cachingConfig, AnnotatedElement ae) {
		Collection<CacheOperation> ops = null;

		Collection<Cacheable> cacheables = AnnotatedElementUtils.getAllMergedAnnotations(ae, Cacheable.class);
		if (!cacheables.isEmpty()) {
			ops = lazyInit(ops);
			for (Cacheable cacheable : cacheables) {
				ops.add(parseCacheableAnnotation(ae, cachingConfig, cacheable));
			}
		}
		Collection<CacheEvict> evicts = AnnotatedElementUtils.getAllMergedAnnotations(ae, CacheEvict.class);
		if (!evicts.isEmpty()) {
			ops = lazyInit(ops);
			for (CacheEvict evict : evicts) {
				ops.add(parseEvictAnnotation(ae, cachingConfig, evict));
			}
		}
		Collection<CachePut> puts = AnnotatedElementUtils.getAllMergedAnnotations(ae, CachePut.class);
		if (!puts.isEmpty()) {
			ops = lazyInit(ops);
			for (CachePut put : puts) {
				ops.add(parsePutAnnotation(ae, cachingConfig, put));
			}
		}
		Collection<Caching> cachings = AnnotatedElementUtils.getAllMergedAnnotations(ae, Caching.class);
		if (!cachings.isEmpty()) {
			ops = lazyInit(ops);
			for (Caching caching : cachings) {
				Collection<CacheOperation> cachingOps = parseCachingAnnotation(ae, cachingConfig, caching);
				if (cachingOps != null) {
					ops.addAll(cachingOps);
				}
			}
		}

		return ops;
	}

	private <T extends Annotation> Collection<CacheOperation> lazyInit(Collection<CacheOperation> ops) {
		return (ops != null ? ops : new ArrayList<CacheOperation>(1));
	}

	CacheableOperation parseCacheableAnnotation(AnnotatedElement ae, DefaultCacheConfig defaultConfig, Cacheable cacheable) {
		CacheableOperation.Builder builder = new CacheableOperation.Builder();

		builder.setName(ae.toString());
		builder.setCacheNames(cacheable.cacheNames());
		builder.setCondition(cacheable.condition());
		builder.setUnless(cacheable.unless());
		builder.setKey(cacheable.key());
		builder.setKeyGenerator(cacheable.keyGenerator());
		builder.setCacheManager(cacheable.cacheManager());
		builder.setCacheResolver(cacheable.cacheResolver());
		builder.setSync(cacheable.sync());

		defaultConfig.applyDefault(builder);
		CacheableOperation op = builder.build();
		validateCacheOperation(ae, op);

		return op;
	}

	CacheEvictOperation parseEvictAnnotation(AnnotatedElement ae, DefaultCacheConfig defaultConfig, CacheEvict cacheEvict) {
		CacheEvictOperation.Builder builder = new CacheEvictOperation.Builder();

		builder.setName(ae.toString());
		builder.setCacheNames(cacheEvict.cacheNames());
		builder.setCondition(cacheEvict.condition());
		builder.setKey(cacheEvict.key());
		builder.setKeyGenerator(cacheEvict.keyGenerator());
		builder.setCacheManager(cacheEvict.cacheManager());
		builder.setCacheResolver(cacheEvict.cacheResolver());
		builder.setCacheWide(cacheEvict.allEntries());
		builder.setBeforeInvocation(cacheEvict.beforeInvocation());

		defaultConfig.applyDefault(builder);
		CacheEvictOperation op = builder.build();
		validateCacheOperation(ae, op);

		return op;
	}

	CacheOperation parsePutAnnotation(AnnotatedElement ae, DefaultCacheConfig defaultConfig, CachePut cachePut) {
		CachePutOperation.Builder builder = new CachePutOperation.Builder();

		builder.setName(ae.toString());
		builder.setCacheNames(cachePut.cacheNames());
		builder.setCondition(cachePut.condition());
		builder.setUnless(cachePut.unless());
		builder.setKey(cachePut.key());
		builder.setKeyGenerator(cachePut.keyGenerator());
		builder.setCacheManager(cachePut.cacheManager());
		builder.setCacheResolver(cachePut.cacheResolver());

		defaultConfig.applyDefault(builder);
		CachePutOperation op = builder.build();
		validateCacheOperation(ae, op);

		return op;
	}

	Collection<CacheOperation> parseCachingAnnotation(AnnotatedElement ae, DefaultCacheConfig defaultConfig, Caching caching) {
		Collection<CacheOperation> ops = null;

		Cacheable[] cacheables = caching.cacheable();
		if (!ObjectUtils.isEmpty(cacheables)) {
			ops = lazyInit(ops);
			for (Cacheable cacheable : cacheables) {
				ops.add(parseCacheableAnnotation(ae, defaultConfig, cacheable));
			}
		}
		CacheEvict[] cacheEvicts = caching.evict();
		if (!ObjectUtils.isEmpty(cacheEvicts)) {
			ops = lazyInit(ops);
			for (CacheEvict cacheEvict : cacheEvicts) {
				ops.add(parseEvictAnnotation(ae, defaultConfig, cacheEvict));
			}
		}
		CachePut[] cachePuts = caching.put();
		if (!ObjectUtils.isEmpty(cachePuts)) {
			ops = lazyInit(ops);
			for (CachePut cachePut : cachePuts) {
				ops.add(parsePutAnnotation(ae, defaultConfig, cachePut));
			}
		}

		return ops;
	}

	/**
	 * 为指定的{@link Class}提供{@link DefaultCacheConfig}实例.
	 * 
	 * @param target 要处理的类级别
	 * 
	 * @return 默认配置 (never {@code null})
	 */
	DefaultCacheConfig getDefaultCacheConfig(Class<?> target) {
		CacheConfig annotation = AnnotatedElementUtils.getMergedAnnotation(target, CacheConfig.class);
		if (annotation != null) {
			return new DefaultCacheConfig(annotation.cacheNames(), annotation.keyGenerator(),
					annotation.cacheManager(), annotation.cacheResolver());
		}
		return new DefaultCacheConfig();
	}

	/**
	 * 验证指定的{@link CacheOperation}.
	 * <p>如果操作的状态无效, 则抛出{@link IllegalStateException}.
	 * 由于可能存在多个默认值源, 因此可确保操作在返回之前处于正确状态.
	 * 
	 * @param ae 缓存操作的带注解元素
	 * @param operation 要验证的{@link CacheOperation}
	 */
	private void validateCacheOperation(AnnotatedElement ae, CacheOperation operation) {
		if (StringUtils.hasText(operation.getKey()) && StringUtils.hasText(operation.getKeyGenerator())) {
			throw new IllegalStateException("Invalid cache annotation configuration on '" +
					ae.toString() + "'. Both 'key' and 'keyGenerator' attributes have been set. " +
					"These attributes are mutually exclusive: either set the SpEL expression used to" +
					"compute the key at runtime or set the name of the KeyGenerator bean to use.");
		}
		if (StringUtils.hasText(operation.getCacheManager()) && StringUtils.hasText(operation.getCacheResolver())) {
			throw new IllegalStateException("Invalid cache annotation configuration on '" +
					ae.toString() + "'. Both 'cacheManager' and 'cacheResolver' attributes have been set. " +
					"These attributes are mutually exclusive: the cache manager is used to configure a" +
					"default cache resolver if none is set. If a cache resolver is set, the cache manager" +
					"won't be used.");
		}
	}

	@Override
	public boolean equals(Object other) {
		return (this == other || other instanceof SpringCacheAnnotationParser);
	}

	@Override
	public int hashCode() {
		return SpringCacheAnnotationParser.class.hashCode();
	}


	/**
	 * 为给定的一组缓存操作提供默认设置.
	 */
	static class DefaultCacheConfig {

		private final String[] cacheNames;

		private final String keyGenerator;

		private final String cacheManager;

		private final String cacheResolver;

		public DefaultCacheConfig() {
			this(null, null, null, null);
		}

		private DefaultCacheConfig(String[] cacheNames, String keyGenerator, String cacheManager, String cacheResolver) {
			this.cacheNames = cacheNames;
			this.keyGenerator = keyGenerator;
			this.cacheManager = cacheManager;
			this.cacheResolver = cacheResolver;
		}

		/**
		 * 将默认值应用于指定的 {@link CacheOperation.Builder}.
		 * 
		 * @param builder 要更新的操作构建器
		 */
		public void applyDefault(CacheOperation.Builder builder) {
			if (builder.getCacheNames().isEmpty() && this.cacheNames != null) {
				builder.setCacheNames(this.cacheNames);
			}
			if (!StringUtils.hasText(builder.getKey()) && !StringUtils.hasText(builder.getKeyGenerator()) &&
					StringUtils.hasText(this.keyGenerator)) {
				builder.setKeyGenerator(this.keyGenerator);
			}

			if (StringUtils.hasText(builder.getCacheManager()) || StringUtils.hasText(builder.getCacheResolver())) {
				// 其中一个是设置的, 所以我们不应该继承任何东西
			}
			else if (StringUtils.hasText(this.cacheResolver)) {
				builder.setCacheResolver(this.cacheResolver);
			}
			else if (StringUtils.hasText(this.cacheManager)) {
				builder.setCacheManager(this.cacheManager);
			}
		}

	}

}
