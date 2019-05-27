package org.springframework.cache.annotation;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.cache.interceptor.AbstractFallbackCacheOperationSource;
import org.springframework.cache.interceptor.CacheOperation;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.cache.interceptor.CacheOperationSource CacheOperationSource}接口的实现, 用于处理注解格式的缓存元数据.
 *
 * <p>该类读取Spring的 {@link Cacheable}, {@link CachePut}和 {@link CacheEvict}注解, 并将相应的缓存操作定义公开给Spring的缓存基础结构.
 * 此类还可以作为自定义{@code CacheOperationSource}的基类.
 */
@SuppressWarnings("serial")
public class AnnotationCacheOperationSource extends AbstractFallbackCacheOperationSource implements Serializable {

	private final boolean publicMethodsOnly;

	private final Set<CacheAnnotationParser> annotationParsers;


	/**
	 * 创建一个默认的AnnotationCacheOperationSource,
	 * 支持带有{@code Cacheable}和{@code CacheEvict}注解的public方法.
	 */
	public AnnotationCacheOperationSource() {
		this(true);
	}

	/**
	 * 创建一个默认的 {@code AnnotationCacheOperationSource},
	 * 支持带有{@code Cacheable}和{@code CacheEvict}注解的public方法.
	 * 
	 * @param publicMethodsOnly 是否仅支持通常用于基于代理的AOP的带注解的public方法,
	 * 或者也支持 protected/private 方法 (通常与AspectJ类编织一起使用)
	 */
	public AnnotationCacheOperationSource(boolean publicMethodsOnly) {
		this.publicMethodsOnly = publicMethodsOnly;
		this.annotationParsers = new LinkedHashSet<CacheAnnotationParser>(1);
		this.annotationParsers.add(new SpringCacheAnnotationParser());
	}

	/**
	 * @param annotationParser 要使用的 CacheAnnotationParser
	 */
	public AnnotationCacheOperationSource(CacheAnnotationParser annotationParser) {
		this.publicMethodsOnly = true;
		Assert.notNull(annotationParser, "CacheAnnotationParser must not be null");
		this.annotationParsers = Collections.singleton(annotationParser);
	}

	/**
	 * @param annotationParsers 要使用的CacheAnnotationParser
	 */
	public AnnotationCacheOperationSource(CacheAnnotationParser... annotationParsers) {
		this.publicMethodsOnly = true;
		Assert.notEmpty(annotationParsers, "At least one CacheAnnotationParser needs to be specified");
		Set<CacheAnnotationParser> parsers = new LinkedHashSet<CacheAnnotationParser>(annotationParsers.length);
		Collections.addAll(parsers, annotationParsers);
		this.annotationParsers = parsers;
	}

	/**
	 * @param annotationParsers 要使用的CacheAnnotationParser
	 */
	public AnnotationCacheOperationSource(Set<CacheAnnotationParser> annotationParsers) {
		this.publicMethodsOnly = true;
		Assert.notEmpty(annotationParsers, "At least one CacheAnnotationParser needs to be specified");
		this.annotationParsers = annotationParsers;
	}


	@Override
	protected Collection<CacheOperation> findCacheOperations(final Class<?> clazz) {
		return determineCacheOperations(new CacheOperationProvider() {
			@Override
			public Collection<CacheOperation> getCacheOperations(CacheAnnotationParser parser) {
				return parser.parseCacheAnnotations(clazz);
			}
		});

	}

	@Override
	protected Collection<CacheOperation> findCacheOperations(final Method method) {
		return determineCacheOperations(new CacheOperationProvider() {
			@Override
			public Collection<CacheOperation> getCacheOperations(CacheAnnotationParser parser) {
				return parser.parseCacheAnnotations(method);
			}
		});
	}

	/**
	 * 确定给定{@link CacheOperationProvider}的缓存操作.
	 * <p>此实现委托给配置{@link CacheAnnotationParser CacheAnnotationParsers}, 用于将已知注解解析为Spring的元数据属性类.
	 * <p>可以重写以支持带有缓存元数据的自定义注解.
	 * 
	 * @param provider 要使用的缓存操作提供程序
	 * 
	 * @return 配置的缓存操作, 或{@code null}
	 */
	protected Collection<CacheOperation> determineCacheOperations(CacheOperationProvider provider) {
		Collection<CacheOperation> ops = null;
		for (CacheAnnotationParser annotationParser : this.annotationParsers) {
			Collection<CacheOperation> annOps = provider.getCacheOperations(annotationParser);
			if (annOps != null) {
				if (ops == null) {
					ops = new ArrayList<CacheOperation>();
				}
				ops.addAll(annOps);
			}
		}
		return ops;
	}

	/**
	 * 默认情况下, 只有public方法可以缓存.
	 */
	@Override
	protected boolean allowPublicMethodsOnly() {
		return this.publicMethodsOnly;
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof AnnotationCacheOperationSource)) {
			return false;
		}
		AnnotationCacheOperationSource otherCos = (AnnotationCacheOperationSource) other;
		return (this.annotationParsers.equals(otherCos.annotationParsers) &&
				this.publicMethodsOnly == otherCos.publicMethodsOnly);
	}

	@Override
	public int hashCode() {
		return this.annotationParsers.hashCode();
	}


	/**
	 * 基于给定{@link CacheAnnotationParser}提供{@link CacheOperation}实例的回调接口.
	 */
	protected interface CacheOperationProvider {

		/**
		 * 返回指定解析器提供的{@link CacheOperation}实例.
		 * 
		 * @param parser 要使用的解析器
		 * 
		 * @return 缓存操作, 或{@code null}
		 */
		Collection<CacheOperation> getCacheOperations(CacheAnnotationParser parser);
	}

}
