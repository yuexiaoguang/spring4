package org.springframework.orm.hibernate3;

import java.sql.SQLException;

import org.hibernate.JDBCException;

import org.springframework.dao.UncategorizedDataAccessException;

/**
 * Hibernate-specific subclass of UncategorizedDataAccessException,
 * for JDBC exceptions that Hibernate wrapped.
 *
 * @deprecated as of Spring 4.3, in favor of Hibernate 4.x/5.x
 */
@Deprecated
@SuppressWarnings("serial")
public class HibernateJdbcException extends UncategorizedDataAccessException {

	public HibernateJdbcException(JDBCException ex) {
		super("JDBC exception on Hibernate data access: SQLException for SQL [" + ex.getSQL() + "]; SQL state [" +
				ex.getSQLState() + "]; error code [" + ex.getErrorCode() + "]; " + ex.getMessage(), ex);
	}

	/**
	 * Return the underlying SQLException.
	 */
	public SQLException getSQLException() {
		return ((JDBCException) getCause()).getSQLException();
	}

	/**
	 * Return the SQL that led to the problem.
	 */
	public String getSql() {
		return ((JDBCException) getCause()).getSQL();
	}

}
