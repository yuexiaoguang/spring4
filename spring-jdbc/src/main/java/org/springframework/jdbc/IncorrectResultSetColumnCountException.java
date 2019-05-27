package org.springframework.jdbc;

import org.springframework.dao.DataRetrievalFailureException;

/**
 * 当结果集没有正确的列数时, 抛出数据访问异常, 例如, 当期望单个列但获得0或多于1列时.
 */
@SuppressWarnings("serial")
public class IncorrectResultSetColumnCountException extends DataRetrievalFailureException {

	private int expectedCount;

	private int actualCount;


	/**
	 * @param expectedCount 预期的列数
	 * @param actualCount 实际的列数
	 */
	public IncorrectResultSetColumnCountException(int expectedCount, int actualCount) {
		super("Incorrect column count: expected " + expectedCount + ", actual " + actualCount);
		this.expectedCount = expectedCount;
		this.actualCount = actualCount;
	}

	/**
	 * @param msg 详细信息
	 * @param expectedCount 预期的列数
	 * @param actualCount 实际的列数
	 */
	public IncorrectResultSetColumnCountException(String msg, int expectedCount, int actualCount) {
		super(msg);
		this.expectedCount = expectedCount;
		this.actualCount = actualCount;
	}


	/**
	 * 返回预期的列数.
	 */
	public int getExpectedCount() {
		return this.expectedCount;
	}

	/**
	 * 返回实际的列数.
	 */
	public int getActualCount() {
		return this.actualCount;
	}

}
