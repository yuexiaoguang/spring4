package org.springframework.scheduling.quartz;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import org.springframework.util.Assert;

/**
 * 简单的Quartz {@link org.quartz.Job}适配器, 它委托给给定的{@link java.lang.Runnable}实例.
 *
 * <p>通常与Runnable实例上的属性注入结合使用, 从而通过Quartz JobDataMap接收参数, 而不是通过JobExecutionContext.
 */
public class DelegatingJob implements Job {

	private final Runnable delegate;


	/**
	 * @param delegate 要委托给的Runnable实现
	 */
	public DelegatingJob(Runnable delegate) {
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
	public void execute(JobExecutionContext context) throws JobExecutionException {
		this.delegate.run();
	}

}
