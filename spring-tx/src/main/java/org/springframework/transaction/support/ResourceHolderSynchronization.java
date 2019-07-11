package org.springframework.transaction.support;

/**
 * {@link TransactionSynchronization}实现, 通过{@link TransactionSynchronizationManager}管理{@link ResourceHolder}.
 */
public abstract class ResourceHolderSynchronization<H extends ResourceHolder, K>
		implements TransactionSynchronization {

	private final H resourceHolder;

	private final K resourceKey;

	private volatile boolean holderActive = true;


	/**
	 * @param resourceHolder 要管理的ResourceHolder
	 * @param resourceKey 绑定ResourceHolder的键
	 */
	public ResourceHolderSynchronization(H resourceHolder, K resourceKey) {
		this.resourceHolder = resourceHolder;
		this.resourceKey = resourceKey;
	}


	@Override
	public void suspend() {
		if (this.holderActive) {
			TransactionSynchronizationManager.unbindResource(this.resourceKey);
		}
	}

	@Override
	public void resume() {
		if (this.holderActive) {
			TransactionSynchronizationManager.bindResource(this.resourceKey, this.resourceHolder);
		}
	}

	@Override
	public void flush() {
		flushResource(this.resourceHolder);
	}

	@Override
	public void beforeCommit(boolean readOnly) {
	}

	@Override
	public void beforeCompletion() {
		if (shouldUnbindAtCompletion()) {
			TransactionSynchronizationManager.unbindResource(this.resourceKey);
			this.holderActive = false;
			if (shouldReleaseBeforeCompletion()) {
				releaseResource(this.resourceHolder, this.resourceKey);
			}
		}
	}

	@Override
	public void afterCommit() {
		if (!shouldReleaseBeforeCompletion()) {
			processResourceAfterCommit(this.resourceHolder);
		}
	}

	@Override
	public void afterCompletion(int status) {
		if (shouldUnbindAtCompletion()) {
			boolean releaseNecessary = false;
			if (this.holderActive) {
				// 线程绑定的资源保存器可能不再可用, 因为afterCompletion可能从另一个线程调用.
				this.holderActive = false;
				TransactionSynchronizationManager.unbindResourceIfPossible(this.resourceKey);
				this.resourceHolder.unbound();
				releaseNecessary = true;
			}
			else {
				releaseNecessary = shouldReleaseAfterCompletion(this.resourceHolder);
			}
			if (releaseNecessary) {
				releaseResource(this.resourceHolder, this.resourceKey);
			}
		}
		else {
			// 可能是预先绑定的资源...
			cleanupResource(this.resourceHolder, this.resourceKey, (status == STATUS_COMMITTED));
		}
		this.resourceHolder.reset();
	}


	/**
	 * 返回此保存器是否应在完成时解绑 (或者应该在事务之后保留绑定到该线程).
	 * <p>默认实现返回 {@code true}.
	 */
	protected boolean shouldUnbindAtCompletion() {
		return true;
	}

	/**
	 * 返回是否应在事务完成之前释放此保存器的资源 ({@code true}), 或者在事务完成之后 ({@code false}).
	 * <p>请注意, 资源只有在从线程中解除绑定时才会被释放 ({@link #shouldUnbindAtCompletion()}).
	 * <p>默认实现返回 {@code true}.
	 */
	protected boolean shouldReleaseBeforeCompletion() {
		return true;
	}

	/**
	 * 返回事务完成后是否应释放此保存器的资源 ({@code true}).
	 * <p>默认实现返回{@code !shouldReleaseBeforeCompletion()}, 如果在完成之前没有尝试则在完成后释放.
	 */
	protected boolean shouldReleaseAfterCompletion(H resourceHolder) {
		return !shouldReleaseBeforeCompletion();
	}

	/**
	 * 刷新给定资源保存器的回调.
	 * 
	 * @param resourceHolder 要刷新的资源保存器
	 */
	protected void flushResource(H resourceHolder) {
	}

	/**
	 * 给定资源保存器的提交后回调.
	 * 仅在资源尚未释放时调用 ({@link #shouldReleaseBeforeCompletion()}).
	 * 
	 * @param resourceHolder 要处理的资源保存器
	 */
	protected void processResourceAfterCommit(H resourceHolder) {
	}

	/**
	 * 释放给定资源 (在从线程解除绑定之后).
	 * 
	 * @param resourceHolder 要处理的资源保存器
	 * @param resourceKey ResourceHolder绑定的键
	 */
	protected void releaseResource(H resourceHolder, K resourceKey) {
	}

	/**
	 * 对给定资源执行清理 (左边绑定到线程).
	 * 
	 * @param resourceHolder 要处理的资源保存器
	 * @param resourceKey ResourceHolder绑定的键
	 * @param committed 事务是否已提交 ({@code true}) 或已回滚 ({@code false})
	 */
	protected void cleanupResource(H resourceHolder, K resourceKey, boolean committed) {
	}

}
