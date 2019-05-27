package org.springframework.aop.target.dynamic;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.TargetSource;

/**
 * 包含可刷新目标对象的抽象{@link org.springframework.aop.TargetSource}实现.
 * 子类可以确定是否需要刷新, 并需要提供新的目标对象.
 *
 * <p>实现{@link Refreshable}接口，以允许显式控制刷新状态.
 */
public abstract class AbstractRefreshableTargetSource implements TargetSource, Refreshable {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	protected Object targetObject;

	private long refreshCheckDelay = -1;

	private long lastRefreshCheck = -1;

	private long lastRefreshTime = -1;

	private long refreshCount = 0;


	/**
	 * 设置刷新检查之间的延迟, in milliseconds.
	 * 默认 -1, 表示没有刷新检查.
	 * <p>请注意, 仅当{@link #requiresRefresh()}返回{@code true}时才会进行实际刷新.
	 */
	public void setRefreshCheckDelay(long refreshCheckDelay) {
		this.refreshCheckDelay = refreshCheckDelay;
	}


	@Override
	public synchronized Class<?> getTargetClass() {
		if (this.targetObject == null) {
			refresh();
		}
		return this.targetObject.getClass();
	}

	/**
	 * Not static.
	 */
	@Override
	public boolean isStatic() {
		return false;
	}

	@Override
	public final synchronized Object getTarget() {
		if ((refreshCheckDelayElapsed() && requiresRefresh()) || this.targetObject == null) {
			refresh();
		}
		return this.targetObject;
	}

	/**
	 * No need to release target.
	 */
	@Override
	public void releaseTarget(Object object) {
	}


	@Override
	public final synchronized void refresh() {
		logger.debug("Attempting to refresh target");

		this.targetObject = freshTarget();
		this.refreshCount++;
		this.lastRefreshTime = System.currentTimeMillis();

		logger.debug("Target refreshed successfully");
	}

	@Override
	public synchronized long getRefreshCount() {
		return this.refreshCount;
	}

	@Override
	public synchronized long getLastRefreshTime() {
		return this.lastRefreshTime;
	}


	private boolean refreshCheckDelayElapsed() {
		if (this.refreshCheckDelay < 0) {
			return false;
		}

		long currentTimeMillis = System.currentTimeMillis();

		if (this.lastRefreshCheck < 0 || currentTimeMillis - this.lastRefreshCheck > this.refreshCheckDelay) {
			// Going to perform a refresh check - update the timestamp.
			this.lastRefreshCheck = currentTimeMillis;
			logger.debug("Refresh check delay elapsed - checking whether refresh is required");
			return true;
		}

		return false;
	}


	/**
	 * 确定是否需要刷新.
	 * 为每次刷新检查调用, 刷新检查延迟结束后.
	 * <p>默认实现始终返回 {@code true}, 每次延迟结束都会触发刷新. 可以被子类覆盖，并对底层目标资源进行适当的检查.
	 * 
	 * @return whether a refresh is required
	 */
	protected boolean requiresRefresh() {
		return true;
	}

	/**
	 * 获取新的目标对象.
	 * <p>仅在刷新检查发现需要刷新时才调用 (即, {@link #requiresRefresh()} 返回 {@code true}).
	 * 
	 * @return 新的目标对象
	 */
	protected abstract Object freshTarget();

}
