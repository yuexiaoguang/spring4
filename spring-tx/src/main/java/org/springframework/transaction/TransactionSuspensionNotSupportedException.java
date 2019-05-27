package org.springframework.transaction;

/**
 * Exception thrown when attempting to suspend an existing transaction
 * but transaction suspension is not supported by the underlying backend.
 */
@SuppressWarnings("serial")
public class TransactionSuspensionNotSupportedException extends CannotCreateTransactionException {

	/**
	 * Constructor for TransactionSuspensionNotSupportedException.
	 * @param msg the detail message
	 */
	public TransactionSuspensionNotSupportedException(String msg) {
		super(msg);
	}

	/**
	 * Constructor for TransactionSuspensionNotSupportedException.
	 * @param msg the detail message
	 * @param cause the root cause from the transaction API in use
	 */
	public TransactionSuspensionNotSupportedException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
