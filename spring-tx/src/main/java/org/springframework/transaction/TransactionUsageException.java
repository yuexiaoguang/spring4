package org.springframework.transaction;

/**
 * 由不恰当使用Spring事务API引起的异常的超类.
 */
@SuppressWarnings("serial")
public class TransactionUsageException extends TransactionException {

	public TransactionUsageException(String msg) {
		super(msg);
	}

	public TransactionUsageException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
