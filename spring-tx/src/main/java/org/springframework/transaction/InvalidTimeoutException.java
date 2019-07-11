package org.springframework.transaction;

/**
 * 指定无效超时时抛出的异常, 即指定的超时超出有效范围, 或事务管理器实现不支持超时.
 */
@SuppressWarnings("serial")
public class InvalidTimeoutException extends TransactionUsageException {

	private int timeout;


	public InvalidTimeoutException(String msg, int timeout) {
		super(msg);
		this.timeout = timeout;
	}

	/**
	 * 返回无效的超时值.
	 */
	public int getTimeout() {
		return timeout;
	}

}
