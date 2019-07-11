package org.springframework.transaction;

/**
 * 尝试提交事务导致意外回滚时抛出.
 */
@SuppressWarnings("serial")
public class UnexpectedRollbackException extends TransactionException {

	public UnexpectedRollbackException(String msg) {
		super(msg);
	}

	public UnexpectedRollbackException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
