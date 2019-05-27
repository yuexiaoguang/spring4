package org.springframework.scheduling.support;

import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.ErrorHandler;
import org.springframework.util.ReflectionUtils;

/**
 * 用于通过错误处理来装饰任务的实用方法.
 *
 * <p><b>NOTE:</b> 此类供Spring的调度器实现内部使用.
 * 它只是public的, 因此可以从其他包中的impl类访问它. 它不适用于一般用途.
 */
public abstract class TaskUtils {

	/**
	 * 一个ErrorHandler策略, 它将记录异常但不执行进一步处理.
	 * 这将抑制错误, 以便不会阻止后续任务的执行.
	 */
	public static final ErrorHandler LOG_AND_SUPPRESS_ERROR_HANDLER = new LoggingErrorHandler();

	/**
	 * 一个ErrorHandler策略, 它将记录错误级别, 然后重新抛出异常.
	 * Note: 这通常会阻止后续计划任务的执行.
	 */
	public static final ErrorHandler LOG_AND_PROPAGATE_ERROR_HANDLER = new PropagatingErrorHandler();


	/**
	 * 装饰任务以进行错误处理.
	 * 如果提供的{@link ErrorHandler}不是{@code null}, 则会使用它.
	 * 否则, 重复任务将默认抑制错误, 而一次性任务将默认传播错误, 因为可能通过返回的{@link Future}预期会出现这些错误.
	 * 在这两种情况下, 都会记录错误.
	 */
	public static DelegatingErrorHandlingRunnable decorateTaskWithErrorHandler(
			Runnable task, ErrorHandler errorHandler, boolean isRepeatingTask) {

		if (task instanceof DelegatingErrorHandlingRunnable) {
			return (DelegatingErrorHandlingRunnable) task;
		}
		ErrorHandler eh = (errorHandler != null ? errorHandler : getDefaultErrorHandler(isRepeatingTask));
		return new DelegatingErrorHandlingRunnable(task, eh);
	}

	/**
	 * 返回默认的{@link ErrorHandler}实现, 该实现基于指示任务是否重复的布尔值.
	 * 对于重复任务, 它将抑制错误; 但对于一次性任务, 它将传播.
	 * 在这两种情况下, 都会记录错误.
	 */
	public static ErrorHandler getDefaultErrorHandler(boolean isRepeatingTask) {
		return (isRepeatingTask ? LOG_AND_SUPPRESS_ERROR_HANDLER : LOG_AND_PROPAGATE_ERROR_HANDLER);
	}


	/**
	 * 一个{@link ErrorHandler}实现, 它将Throwable记录在错误级别.
	 * 它不执行任何其他错误处理. 当抑制错误是预期的行为时, 这可能很有用.
	 */
	private static class LoggingErrorHandler implements ErrorHandler {

		private final Log logger = LogFactory.getLog(LoggingErrorHandler.class);

		@Override
		public void handleError(Throwable t) {
			if (logger.isErrorEnabled()) {
				logger.error("Unexpected error occurred in scheduled task.", t);
			}
		}
	}


	/**
	 * 一个{@link ErrorHandler}实现, 它将Throwable记录在错误级别, 然后传播它.
	 */
	private static class PropagatingErrorHandler extends LoggingErrorHandler {

		@Override
		public void handleError(Throwable t) {
			super.handleError(t);
			ReflectionUtils.rethrowRuntimeException(t);
		}
	}
}
