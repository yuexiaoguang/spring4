package org.springframework.cache.transaction;

import java.util.concurrent.Callable;

import org.springframework.cache.Cache;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * 缓存装饰器, 它将{@link #put}, {@link #evict}和{@link #clear}操作与Spring管理的事务同步
 * (通过Spring的{@link TransactionSynchronizationManager},
 * 仅在事务成功提交后的阶段执行实际缓存put/evict/clear操作.
 * 如果没有活动的事务, {@link #put}, {@link #evict}和{@link #clear}操作将像往常一样立即执行.
 *
 * <p>使用更积极的操作, 例如{@link #putIfAbsent}, 不能延迟到正在运行的事务提交后的阶段.
 * Use these with care.
 */
public class TransactionAwareCacheDecorator implements Cache {

	private final Cache targetCache;


	/**
	 * @param targetCache 要装饰的目标Cache
	 */
	public TransactionAwareCacheDecorator(Cache targetCache) {
		Assert.notNull(targetCache, "Target Cache must not be null");
		this.targetCache = targetCache;
	}

	/**
	 * 返回此Cache应委托给的目标Cache.
	 */
	public Cache getTargetCache() {
		return this.targetCache;
	}

	@Override
	public String getName() {
		return this.targetCache.getName();
	}

	@Override
	public Object getNativeCache() {
		return this.targetCache.getNativeCache();
	}

	@Override
	public ValueWrapper get(Object key) {
		return this.targetCache.get(key);
	}

	@Override
	public <T> T get(Object key, Class<T> type) {
		return this.targetCache.get(key, type);
	}

	@Override
	public <T> T get(Object key, Callable<T> valueLoader) {
		return this.targetCache.get(key, valueLoader);
	}

	@Override
	public void put(final Object key, final Object value) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
				@Override
				public void afterCommit() {
					TransactionAwareCacheDecorator.this.targetCache.put(key, value);
				}
			});
		}
		else {
			this.targetCache.put(key, value);
		}
	}

	@Override
	public ValueWrapper putIfAbsent(final Object key, final Object value) {
		return this.targetCache.putIfAbsent(key, value);
	}

	@Override
	public void evict(final Object key) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
				@Override
				public void afterCommit() {
					TransactionAwareCacheDecorator.this.targetCache.evict(key);
				}
			});
		}
		else {
			this.targetCache.evict(key);
		}
	}

	@Override
	public void clear() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
				@Override
				public void afterCommit() {
					targetCache.clear();
				}
			});
		}
		else {
			this.targetCache.clear();
		}
	}

}
