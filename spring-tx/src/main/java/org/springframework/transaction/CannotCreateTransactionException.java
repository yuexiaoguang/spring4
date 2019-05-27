package org.springframework.transaction;

/**
 * Exception thrown when a transaction can't be created using an
 * underlying transaction API such as JTA.
 */
@SuppressWarnings("serial")
public class CannotCreateTransactionException extends TransactionException {

	/**
	 * Constructor for CannotCreateTransactionException.
	 * @param msg the detail message
	 */
	public CannotCreateTransactionException(String msg) {
		super(msg);
	}

	/**
	 * Constructor for CannotCreateTransactionException.
	 * @param msg the detail message
	 * @param cause the root cause from the transaction API in use
	 */
	public CannotCreateTransactionException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
