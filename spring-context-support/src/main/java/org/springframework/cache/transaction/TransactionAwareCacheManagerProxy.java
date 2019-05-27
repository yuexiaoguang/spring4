package org.springframework.cache.transaction;

import java.util.Collection;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.util.Assert;

/**
 * 目标{@link CacheManager}的代理, 公开事务感知{@link Cache}对象, 将{@link Cache#put}操作与Spring管理的事务同步
 * (通过Spring的 {@link org.springframework.transaction.support.TransactionSynchronizationManager},
 * 仅在事务成功提交后的阶段执行实际缓存put操作.
 * 如果没有活动的事务, 将像往常一样立即执行{@link Cache#put}操作.
 */
public class TransactionAwareCacheManagerProxy implements CacheManager, InitializingBean {

	private CacheManager targetCacheManager;


	/**
	 * 通过{@link #setTargetCacheManager} bean属性设置目标CacheManager.
	 */
	public TransactionAwareCacheManagerProxy() {
	}

	/**
	 * @param targetCacheManager 要代理的目标CacheManager
	 */
	public TransactionAwareCacheManagerProxy(CacheManager targetCacheManager) {
		Assert.notNull(targetCacheManager, "Target CacheManager must not be null");
		this.targetCacheManager = targetCacheManager;
	}


	/**
	 * 设置要代理的目标CacheManager.
	 */
	public void setTargetCacheManager(CacheManager targetCacheManager) {
		this.targetCacheManager = targetCacheManager;
	}

	@Override
	public void afterPropertiesSet() {
		if (this.targetCacheManager == null) {
			throw new IllegalArgumentException("Property 'targetCacheManager' is required");
		}
	}


	@Override
	public Cache getCache(String name) {
		return new TransactionAwareCacheDecorator(this.targetCacheManager.getCache(name));
	}

	@Override
	public Collection<String> getCacheNames() {
		return this.targetCacheManager.getCacheNames();
	}

}
