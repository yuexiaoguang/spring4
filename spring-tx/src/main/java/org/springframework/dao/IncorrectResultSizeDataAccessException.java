package org.springframework.dao;

/**
 * 当结果不是预期大小时抛出的数据访问异常, 例如, 当期望单行但获得0或多于1行时.
 */
@SuppressWarnings("serial")
public class IncorrectResultSizeDataAccessException extends DataRetrievalFailureException {

	private int expectedSize;

	private int actualSize;


	public IncorrectResultSizeDataAccessException(int expectedSize) {
		super("Incorrect result size: expected " + expectedSize);
		this.expectedSize = expectedSize;
		this.actualSize = -1;
	}

	/**
	 * @param expectedSize 预期的结果大小
	 * @param actualSize 实际结果大小 (或 -1 未知)
	 */
	public IncorrectResultSizeDataAccessException(int expectedSize, int actualSize) {
		super("Incorrect result size: expected " + expectedSize + ", actual " + actualSize);
		this.expectedSize = expectedSize;
		this.actualSize = actualSize;
	}

	/**
	 * @param msg 详细信息
	 * @param expectedSize 预期的结果大小
	 */
	public IncorrectResultSizeDataAccessException(String msg, int expectedSize) {
		super(msg);
		this.expectedSize = expectedSize;
		this.actualSize = -1;
	}

	/**
	 * @param msg 详细信息
	 * @param expectedSize 预期的结果大小
	 * @param ex 包装的异常
	 */
	public IncorrectResultSizeDataAccessException(String msg, int expectedSize, Throwable ex) {
		super(msg, ex);
		this.expectedSize = expectedSize;
		this.actualSize = -1;
	}

	/**
	 * @param msg 详细信息
	 * @param expectedSize 预期的结果大小
	 * @param actualSize 实际结果大小 (或 -1 未知)
	 */
	public IncorrectResultSizeDataAccessException(String msg, int expectedSize, int actualSize) {
		super(msg);
		this.expectedSize = expectedSize;
		this.actualSize = actualSize;
	}

	/**
	 * @param msg 详细信息
	 * @param expectedSize 预期的结果大小
	 * @param actualSize 实际结果大小 (或 -1 未知)
	 * @param ex 包装的异常
	 */
	public IncorrectResultSizeDataAccessException(String msg, int expectedSize, int actualSize, Throwable ex) {
		super(msg, ex);
		this.expectedSize = expectedSize;
		this.actualSize = actualSize;
	}


	/**
	 * 返回预期的结果大小.
	 */
	public int getExpectedSize() {
		return this.expectedSize;
	}

	/**
	 * 返回实际的结果大小 (或 -1 未知).
	 */
	public int getActualSize() {
		return this.actualSize;
	}

}
