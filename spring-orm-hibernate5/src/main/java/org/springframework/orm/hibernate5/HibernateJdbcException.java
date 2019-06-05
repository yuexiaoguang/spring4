package org.springframework.orm.hibernate5;

import java.sql.SQLException;

import org.hibernate.JDBCException;

import org.springframework.dao.UncategorizedDataAccessException;

/**
 * 特定于Hibernate的UncategorizedDataAccessException子类, 用于Hibernate包装的JDBC异常.
 */
@SuppressWarnings("serial")
public class HibernateJdbcException extends UncategorizedDataAccessException {

	public HibernateJdbcException(JDBCException ex) {
		super("JDBC exception on Hibernate data access: SQLException for SQL [" + ex.getSQL() + "]; SQL state [" +
				ex.getSQLState() + "]; error code [" + ex.getErrorCode() + "]; " + ex.getMessage(), ex);
	}

	/**
	 * 返回底层SQLException.
	 */
	public SQLException getSQLException() {
		return ((JDBCException) getCause()).getSQLException();
	}

	/**
	 * 返回导致问题的SQL.
	 */
	public String getSql() {
		return ((JDBCException) getCause()).getSQL();
	}

}
