package org.springframework.util;

/**
 * 处理错误的策略.
 * 这对于处理在已提交给TaskScheduler的任务的异步执行期间发生的错误特别有用.
 * 在这种情况下, 可能无法将错误抛给原始调用者.
 */
public interface ErrorHandler {

	/**
	 * 处理给定的错误, 可能将其重新抛出为致命异常.
	 */
	void handleError(Throwable t);

}
