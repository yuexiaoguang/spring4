package org.springframework.transaction;

/**
 * 尝试使用嵌套事务, 但底层后端不支持嵌套事务时抛出的异常.
 */
@SuppressWarnings("serial")
public class NestedTransactionNotSupportedException extends CannotCreateTransactionException {

	public NestedTransactionNotSupportedException(String msg) {
		super(msg);
	}

	public NestedTransactionNotSupportedException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
