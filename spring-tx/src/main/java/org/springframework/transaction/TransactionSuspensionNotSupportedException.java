package org.springframework.transaction;

/**
 * 尝试挂起现有事务, 但底层后端不支持事务暂停时抛出的异常.
 */
@SuppressWarnings("serial")
public class TransactionSuspensionNotSupportedException extends CannotCreateTransactionException {

	public TransactionSuspensionNotSupportedException(String msg) {
		super(msg);
	}

	public TransactionSuspensionNotSupportedException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
