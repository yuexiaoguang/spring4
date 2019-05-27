package org.springframework.jdbc;

import java.sql.SQLException;

import org.springframework.dao.UncategorizedDataAccessException;

/**
 * 当无法将SQLException分类为一个通用数据访问异常时抛出异常.
 */
@SuppressWarnings("serial")
public class UncategorizedSQLException extends UncategorizedDataAccessException {

	/** 导致了这个问题的SQL */
	private final String sql;


	/**
	 * @param task 当前任务的名称
	 * @param sql 违规的SQL语句
	 * @param ex 根异常
	 */
	public UncategorizedSQLException(String task, String sql, SQLException ex) {
		super(task + "; uncategorized SQLException for SQL [" + sql + "]; SQL state [" +
				ex.getSQLState() + "]; error code [" + ex.getErrorCode() + "]; " + ex.getMessage(), ex);
		this.sql = sql;
	}


	/**
	 * 返回底层SQLException.
	 */
	public SQLException getSQLException() {
		return (SQLException) getCause();
	}

	/**
	 * 返回导致问题的SQL.
	 */
	public String getSql() {
		return this.sql;
	}

}
