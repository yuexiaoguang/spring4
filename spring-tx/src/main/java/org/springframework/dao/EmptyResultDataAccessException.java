package org.springframework.dao;

/**
 * Data access exception thrown when a result was expected to have at least
 * one row (or element) but zero rows (or elements) were actually returned.
 */
@SuppressWarnings("serial")
public class EmptyResultDataAccessException extends IncorrectResultSizeDataAccessException {

	/**
	 * Constructor for EmptyResultDataAccessException.
	 * @param expectedSize the expected result size
	 */
	public EmptyResultDataAccessException(int expectedSize) {
		super(expectedSize, 0);
	}

	/**
	 * Constructor for EmptyResultDataAccessException.
	 * @param msg the detail message
	 * @param expectedSize the expected result size
	 */
	public EmptyResultDataAccessException(String msg, int expectedSize) {
		super(msg, expectedSize, 0);
	}

	/**
	 * Constructor for EmptyResultDataAccessException.
	 * @param msg the detail message
	 * @param expectedSize the expected result size
	 * @param ex the wrapped exception
	 */
	public EmptyResultDataAccessException(String msg, int expectedSize, Throwable ex) {
		super(msg, expectedSize, 0, ex);
	}

}
