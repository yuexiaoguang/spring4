package org.springframework.jca.work;

import javax.resource.spi.work.Work;

import org.springframework.util.Assert;

/**
 * 委托给给定Runnable的简单Work适配器.
 */
public class DelegatingWork implements Work {

	private final Runnable delegate;


	/**
	 * @param delegate 要委托给的Runnable实现
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
	 * 此实现为空, 因为希望Runnable根据某些特定的关闭信号终止.
	 */
	@Override
	public void release() {
	}

}
