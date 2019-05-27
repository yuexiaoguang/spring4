package org.springframework.dao;

/**
 * Data access exception thrown when a resource fails completely and the failure is permanent.
 */
@SuppressWarnings("serial")
public class NonTransientDataAccessResourceException extends NonTransientDataAccessException {

	/**
	 * Constructor for NonTransientDataAccessResourceException.
	 * @param msg the detail message
	 */
	public NonTransientDataAccessResourceException(String msg) {
		super(msg);
	}

	/**
	 * Constructor for NonTransientDataAccessResourceException.
	 * @param msg the detail message
	 * @param cause the root cause from the data access API in use
	 */
	public NonTransientDataAccessResourceException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
