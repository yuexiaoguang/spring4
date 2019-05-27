package org.springframework.core.task;

/**
 * {@link AsyncTaskExecutor}由于指定的超时, 而拒绝接受给定任务执行时抛出异常.
 */
@SuppressWarnings("serial")
public class TaskTimeoutException extends TaskRejectedException {

	public TaskTimeoutException(String msg) {
		super(msg);
	}

	public TaskTimeoutException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
