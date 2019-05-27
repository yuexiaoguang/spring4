package org.springframework.scheduling.commonj;

import commonj.work.Work;

import org.springframework.scheduling.SchedulingAwareRunnable;
import org.springframework.util.Assert;

/**
 * 委托给给定Runnable的简单Work适配器.
 */
public class DelegatingWork implements Work {

	private final Runnable delegate;


	/**
	 * @param delegate 要委派给的Runnable实现 (可能是用于扩展支持的SchedulingAwareRunnable)
	 */
	public DelegatingWork(Runnable delegate) {
		Assert.notNull(delegate, "Delegate must not be null");
		this.delegate = delegate;
	}

	/**
	 * 返回包装的Runnable实现.
	 */
	public final Runnable getDelegate() {
		return this.delegate;
	}


	/**
	 * 委托执行给底层Runnable.
	 */
	@Override
	public void run() {
		this.delegate.run();
	}

	/**
	 * 此实现委托给
	 * {@link org.springframework.scheduling.SchedulingAwareRunnable#isLongLived()}.
	 */
	@Override
	public boolean isDaemon() {
		return (this.delegate instanceof SchedulingAwareRunnable &&
				((SchedulingAwareRunnable) this.delegate).isLongLived());
	}

	/**
	 * 此实现为空, 因为希望Runnable根据某些特定的关闭信号终止.
	 */
	@Override
	public void release() {
	}
}
