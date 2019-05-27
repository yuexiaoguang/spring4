package org.springframework.core.task;

import java.util.concurrent.RejectedExecutionException;

/**
 * 当{@link TaskExecutor}拒绝接受给定任务执行时抛出异常.
 */
@SuppressWarnings("serial")
public class TaskRejectedException extends RejectedExecutionException {

	public TaskRejectedException(String msg) {
		super(msg);
	}

	public TaskRejectedException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
