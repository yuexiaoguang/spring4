package org.springframework.transaction;

import org.springframework.util.Assert;

/**
 * 遇到常规事务系统错误时抛出的异常, 例如在提交或回滚时.
 */
@SuppressWarnings("serial")
public class TransactionSystemException extends TransactionException {

	private Throwable applicationException;


	public TransactionSystemException(String msg) {
		super(msg);
	}

	public TransactionSystemException(String msg, Throwable cause) {
		super(msg, cause);
	}


	/**
	 * 设置在此事务异常之前抛出的应用程序异常, 尽管覆盖了TransactionSystemException, 仍保留原始异常.
	 * 
	 * @param ex 应用程序异常
	 * 
	 * @throws IllegalStateException 如果此TransactionSystemException已经存在应用程序异常
	 */
	public void initApplicationException(Throwable ex) {
		Assert.notNull(ex, "Application exception must not be null");
		if (this.applicationException != null) {
			throw new IllegalStateException("Already holding an application exception: " + this.applicationException);
		}
		this.applicationException = ex;
	}

	/**
	 * 返回在此事务异常之前抛出的应用程序异常.
	 * 
	 * @return 应用程序异常, 或{@code null}
	 */
	public final Throwable getApplicationException() {
		return this.applicationException;
	}

	/**
	 * 返回在失败事务中第一个抛出的异常: i.e. 应用程序异常, 或TransactionSystemException自身的原因.
	 * 
	 * @return 原始异常, 或{@code null}
	 */
	public Throwable getOriginalException() {
		return (this.applicationException != null ? this.applicationException : getCause());
	}

	@Override
	public boolean contains(Class<?> exType) {
		return super.contains(exType) || (exType != null && exType.isInstance(this.applicationException));
	}

}
