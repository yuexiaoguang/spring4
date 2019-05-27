package org.springframework.scheduling.config;

import java.util.concurrent.ScheduledFuture;

/**
 * 计划任务的表示, 用作计划方法的返回值.
 */
public final class ScheduledTask {

	volatile ScheduledFuture<?> future;


	ScheduledTask() {
	}


	/**
	 * 触发此计划任务的取消.
	 */
	public void cancel() {
		ScheduledFuture<?> future = this.future;
		if (future != null) {
			future.cancel(true);
		}
	}

}
