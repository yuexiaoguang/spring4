package org.springframework.scheduling.support;

import java.lang.reflect.UndeclaredThrowableException;

import org.springframework.util.Assert;
import org.springframework.util.ErrorHandler;

/**
 * Runnable包装器, 捕获从其委托Runnable抛出的任何异常或错误, 并允许{@link ErrorHandler}处理它.
 */
public class DelegatingErrorHandlingRunnable implements Runnable {

	private final Runnable delegate;

	private final ErrorHandler errorHandler;


	/**
	 * @param delegate 要委托给的Runnable实现
	 * @param errorHandler 用于处理异常的ErrorHandler
	 */
	public DelegatingErrorHandlingRunnable(Runnable delegate, ErrorHandler errorHandler) {
		Assert.notNull(delegate, "Delegate must not be null");
		Assert.notNull(errorHandler, "ErrorHandler must not be null");
		this.delegate = delegate;
		this.errorHandler = errorHandler;
	}

	@Override
	public void run() {
		try {
			this.delegate.run();
		}
		catch (UndeclaredThrowableException ex) {
			this.errorHandler.handleError(ex.getUndeclaredThrowable());
		}
		catch (Throwable ex) {
			this.errorHandler.handleError(ex);
		}
	}

	@Override
	public String toString() {
		return "DelegatingErrorHandlingRunnable for " + this.delegate;
	}

}
