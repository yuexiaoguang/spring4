package org.springframework.transaction;

/**
 * 无法使用底层事务API (如JTA)创建事务时抛出的异常.
 */
@SuppressWarnings("serial")
public class CannotCreateTransactionException extends TransactionException {

	public CannotCreateTransactionException(String msg) {
		super(msg);
	}

	public CannotCreateTransactionException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
