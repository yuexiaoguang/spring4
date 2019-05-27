package org.springframework.messaging.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * 代理目标DestinationResolver的{@link DestinationResolver}实现, 缓存其{@link #resolveDestination}结果.
 * 如果目标解析过程很昂贵 (e.g. 目标必须通过外部系统解析) 并且解析结果又很稳定, 则此类缓存特别有用.
 */
public class CachingDestinationResolverProxy<D> implements DestinationResolver<D>, InitializingBean {

	private final Map<String, D> resolvedDestinationCache = new ConcurrentHashMap<String, D>();

	private DestinationResolver<D> targetDestinationResolver;


	/**
	 * 通过{@link #setTargetDestinationResolver} bean属性设置目标DestinationResolver.
	 */
	public CachingDestinationResolverProxy() {
	}

	/**
	 * 使用给定的目标DestinationResolver实际解析目标.
	 * 
	 * @param targetDestinationResolver 要委托给的目标DestinationResolver
	 */
	public CachingDestinationResolverProxy(DestinationResolver<D> targetDestinationResolver) {
		Assert.notNull(targetDestinationResolver, "Target DestinationResolver must not be null");
		this.targetDestinationResolver = targetDestinationResolver;
	}


	/**
	 * 设置要委托给的目标DestinationResolver.
	 */
	public void setTargetDestinationResolver(DestinationResolver<D> targetDestinationResolver) {
		this.targetDestinationResolver = targetDestinationResolver;
	}

	@Override
	public void afterPropertiesSet() {
		if (this.targetDestinationResolver == null) {
			throw new IllegalArgumentException("Property 'targetDestinationResolver' is required");
		}
	}


	/**
	 * 如果目标DestinationResolver实现成功解析, 则解析并缓存目标.
	 * 
	 * @param name 要解析的目标名称
	 * 
	 * @return 当前已解析的目标或已缓存的目标
	 * @throws DestinationResolutionException 如果目标DestinationResolver在目标解析期间报告错误
	 */
	@Override
	public D resolveDestination(String name) throws DestinationResolutionException {
		D destination = this.resolvedDestinationCache.get(name);
		if (destination == null) {
			destination = this.targetDestinationResolver.resolveDestination(name);
			this.resolvedDestinationCache.put(name, destination);
		}
		return destination;
	}

}
