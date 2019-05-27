package org.springframework.dao;

/**
 * Exception thrown on concurrency failure.
 *
 * <p>This exception should be subclassed to indicate the type of failure:
 * optimistic locking, failure to acquire lock, etc.
 */
@SuppressWarnings("serial")
public class ConcurrencyFailureException extends TransientDataAccessException {

	/**
	 * Constructor for ConcurrencyFailureException.
	 * @param msg the detail message
	 */
	public ConcurrencyFailureException(String msg) {
		super(msg);
	}

	/**
	 * Constructor for ConcurrencyFailureException.
	 * @param msg the detail message
	 * @param cause the root cause from the data access API in use
	 */
	public ConcurrencyFailureException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
