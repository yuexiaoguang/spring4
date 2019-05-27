package org.springframework.scheduling.commonj;

import commonj.timers.Timer;
import commonj.timers.TimerListener;

import org.springframework.util.Assert;

/**
 * 简单的TimerListener适配器, 它委托给给定的Runnable.
 */
public class DelegatingTimerListener implements TimerListener {

	private final Runnable runnable;


	/**
	 * @param runnable 要委托给的Runnable实现
	 */
	public DelegatingTimerListener(Runnable runnable) {
		Assert.notNull(runnable, "Runnable is required");
		this.runnable = runnable;
	}


	/**
	 * 委托执行给底层Runnable.
	 */
	@Override
	public void timerExpired(Timer timer) {
		this.runnable.run();
	}
}
