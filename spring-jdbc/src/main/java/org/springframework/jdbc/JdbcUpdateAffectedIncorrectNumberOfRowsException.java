package org.springframework.jdbc;

import org.springframework.dao.IncorrectUpdateSemanticsDataAccessException;

/**
 * JDBC更新影响意外行数时抛出异常.
 * 通常更新会影响单个行, 这意味着如果它影响多行, 则会出错.
 */
@SuppressWarnings("serial")
public class JdbcUpdateAffectedIncorrectNumberOfRowsException extends IncorrectUpdateSemanticsDataAccessException {

	/** 应受影响的行数 */
	private int expected;

	/** 实际受影响的行数 */
	private int actual;


	/**
	 * @param sql 要执行的SQL
	 * @param expected 受影响的预期行数
	 * @param actual 受影响的实际行数
	 */
	public JdbcUpdateAffectedIncorrectNumberOfRowsException(String sql, int expected, int actual) {
		super("SQL update '" + sql + "' affected " + actual + " rows, not " + expected + " as expected");
		this.expected = expected;
		this.actual = actual;
	}


	/**
	 * 返回应该受影响的行数.
	 */
	public int getExpectedRowsAffected() {
		return this.expected;
	}

	/**
	 * 返回实际受影响的行数.
	 */
	public int getActualRowsAffected() {
		return this.actual;
	}

	@Override
	public boolean wasDataUpdated() {
		return (getActualRowsAffected() > 0);
	}
}
