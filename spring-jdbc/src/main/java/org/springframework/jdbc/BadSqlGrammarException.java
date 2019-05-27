package org.springframework.jdbc;

import java.sql.SQLException;

import org.springframework.dao.InvalidDataAccessResourceUsageException;

/**
 * 指定的SQL无效时抛出异常. 此类异常始终具有{@code java.sql.SQLException}根本原因.
 *
 * <p>没有这样的表, 没有这样的列等可能有子类.
 * 自定义SQLExceptionTranslator可以创建更具体的异常, 而不会影响使用此类的代码.
 */
@SuppressWarnings("serial")
public class BadSqlGrammarException extends InvalidDataAccessResourceUsageException {

	private String sql;


	/**
	 * @param task 当前任务的名称
	 * @param sql 违规的SQL语句
	 * @param ex 根本原因
	 */
	public BadSqlGrammarException(String task, String sql, SQLException ex) {
		super(task + "; bad SQL grammar [" + sql + "]", ex);
		this.sql = sql;
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
