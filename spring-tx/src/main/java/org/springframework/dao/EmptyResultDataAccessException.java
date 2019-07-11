package org.springframework.dao;

/**
 * 当预期结果至少有一行 (或元素), 但实际返回零行 (或元素)时抛出的数据访问异常.
 */
@SuppressWarnings("serial")
public class EmptyResultDataAccessException extends IncorrectResultSizeDataAccessException {

	public EmptyResultDataAccessException(int expectedSize) {
		super(expectedSize, 0);
	}

	/**
	 * @param msg 详细信息
	 * @param expectedSize 预期的结果大小
	 */
	public EmptyResultDataAccessException(String msg, int expectedSize) {
		super(msg, expectedSize, 0);
	}

	/**
	 * @param msg 详细信息
	 * @param expectedSize 预期的结果大小
	 * @param ex 包装的异常
	 */
	public EmptyResultDataAccessException(String msg, int expectedSize, Throwable ex) {
		super(msg, expectedSize, 0, ex);
	}

}
