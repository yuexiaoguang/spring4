package org.springframework.dao;

/**
 * Exception thrown when the underlying resource denied a permission
 * to access a specific element, such as a specific database table.
 */
@SuppressWarnings("serial")
public class PermissionDeniedDataAccessException extends NonTransientDataAccessException {

	/**
	 * Constructor for PermissionDeniedDataAccessException.
	 * @param msg the detail message
	 * @param cause the root cause from the underlying data access API,
	 * such as JDBC
	 */
	public PermissionDeniedDataAccessException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
