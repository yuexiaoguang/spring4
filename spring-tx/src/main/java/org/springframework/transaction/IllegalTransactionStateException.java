package org.springframework.transaction;

/**
 * 根据适用的事务传播行为, 当事务的存在或不存在等于非法状态时抛出的异常.
 */
@SuppressWarnings("serial")
public class IllegalTransactionStateException extends TransactionUsageException {

	public IllegalTransactionStateException(String msg) {
		super(msg);
	}

	public IllegalTransactionStateException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
