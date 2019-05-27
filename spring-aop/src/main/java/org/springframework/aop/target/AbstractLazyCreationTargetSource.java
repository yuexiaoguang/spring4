package org.springframework.aop.target;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.TargetSource;

/**
 * {@link org.springframework.aop.TargetSource}实现, 将延迟创建一个用户管理的对象.
 *
 * <p>用户通过实现{@link #createObject()}方法来控制延迟目标对象的创建. 这个{@code TargetSource}将在第一次访问代理时调用此方法.
 *
 * <p>当您需要将对某个依赖项的引用传递给对象时很有用, 但实际上并不希望在首次使用之前创建依赖项.
 * 典型的情况是与远程资源的连接.
 */
public abstract class AbstractLazyCreationTargetSource implements TargetSource {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	/** 延迟初始化的目标对象 */
	private Object lazyTarget;


	/**
	 * 返回是否已获取此TargetSource的延迟目标对象.
	 */
	public synchronized boolean isInitialized() {
		return (this.lazyTarget != null);
	}

	/**
	 * 如果目标是{@code null}, 则此默认实现返回{@code null} (它尚未初始化),
	 * 如果目标已经初始化, 则为目标类.
	 * <p>子类可能希望覆盖此方法, 以便在目标仍为{@code null}时提供有意义的值.
	 */
	@Override
	public synchronized Class<?> getTargetClass() {
		return (this.lazyTarget != null ? this.lazyTarget.getClass() : null);
	}

	@Override
	public boolean isStatic() {
		return false;
	}

	/**
	 * 返回延迟初始化的目标对象, 如果它已经不存在, 则即时创建它.
	 */
	@Override
	public synchronized Object getTarget() throws Exception {
		if (this.lazyTarget == null) {
			logger.debug("Initializing lazy target object");
			this.lazyTarget = createObject();
		}
		return this.lazyTarget;
	}

	@Override
	public void releaseTarget(Object target) throws Exception {
		// nothing to do
	}


	/**
	 * 子类应该实现此方法以返回延迟初始化对象.
	 * 第一次调用代理时调用.
	 * 
	 * @return 创建的对象
	 * @throws Exception 如果创建失败
	 */
	protected abstract Object createObject() throws Exception;

}
