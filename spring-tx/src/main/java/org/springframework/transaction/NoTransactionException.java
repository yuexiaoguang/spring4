package org.springframework.transaction;

/**
 * 尝试依赖现有事务(例如设置回滚状态)但没有现有事务时抛出的异常.
 * 表示非法使用事务API.
 */
@SuppressWarnings("serial")
public class NoTransactionException extends TransactionUsageException {

	public NoTransactionException(String msg) {
		super(msg);
	}

	public NoTransactionException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
