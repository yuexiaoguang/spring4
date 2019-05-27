package org.springframework.jdbc;

import java.sql.SQLException;

import org.springframework.dao.InvalidDataAccessResourceUsageException;

/**
 * 以无效方式访问ResultSet时抛出异常.
 * 此类异常始终具有{@code java.sql.SQLException}根本原因.
 *
 * <p>当指定了无效的ResultSet列索引或名称时, 通常会发生这种情况. 也可由断开连接的SqlRowSets引发.
 */
@SuppressWarnings("serial")
public class InvalidResultSetAccessException extends InvalidDataAccessResourceUsageException {

	private String sql;


	/**
	 * @param task 当前任务的名称
	 * @param sql 违规的SQL语句
	 * @param ex 根本原因
	 */
	public InvalidResultSetAccessException(String task, String sql, SQLException ex) {
		super(task + "; invalid ResultSet access for SQL [" + sql + "]", ex);
		this.sql = sql;
	}

	public InvalidResultSetAccessException(SQLException ex) {
		super(ex.getMessage(), ex);
	}


	/**
	 * 返回包装的SQLException.
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
