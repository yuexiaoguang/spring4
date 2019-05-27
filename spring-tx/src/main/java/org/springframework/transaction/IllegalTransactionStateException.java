package org.springframework.transaction;

/**
 * Exception thrown when the existence or non-existence of a transaction
 * amounts to an illegal state according to the transaction propagation
 * behavior that applies.
 */
@SuppressWarnings("serial")
public class IllegalTransactionStateException extends TransactionUsageException {

	/**
	 * Constructor for IllegalTransactionStateException.
	 * @param msg the detail message
	 */
	public IllegalTransactionStateException(String msg) {
		super(msg);
	}

	/**
	 * Constructor for IllegalTransactionStateException.
	 * @param msg the detail message
	 * @param cause the root cause from the transaction API in use
	 */
	public IllegalTransactionStateException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
