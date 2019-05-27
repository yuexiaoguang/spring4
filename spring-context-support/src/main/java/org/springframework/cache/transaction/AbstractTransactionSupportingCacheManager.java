package org.springframework.cache.transaction;

import org.springframework.cache.Cache;
import org.springframework.cache.support.AbstractCacheManager;

/**
 * 希望支持Spring管理事务的内置感知的CacheManager实现的基类.
 * 这通常需要通过{@link #setTransactionAware} bean属性显式打开.
 */
public abstract class AbstractTransactionSupportingCacheManager extends AbstractCacheManager {

	private boolean transactionAware = false;


	/**
	 * 设置此CacheManager是否应公开事务感知的Cache对象.
	 * <p>默认"false".
	 * 将此设置为"true"以将缓存put/evict操作与正在进行的Spring管理事务同步,
	 * 仅在事务成功提交后的阶段执行实际缓存put/evict操作.
	 */
	public void setTransactionAware(boolean transactionAware) {
		this.transactionAware = transactionAware;
	}

	/**
	 * 返回此CacheManager是否已配置为可识别事务.
	 */
	public boolean isTransactionAware() {
		return this.transactionAware;
	}


	@Override
	protected Cache decorateCache(Cache cache) {
		return (isTransactionAware() ? new TransactionAwareCacheDecorator(cache) : cache);
	}

}
