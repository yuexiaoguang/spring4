package org.springframework.jdbc;

import java.sql.SQLException;

import org.springframework.dao.DataAccessResourceFailureException;

/**
 * 当我们无法使用JDBC连接到RDBMS时抛出的致命异常.
 */
@SuppressWarnings("serial")
public class CannotGetJdbcConnectionException extends DataAccessResourceFailureException {

	public CannotGetJdbcConnectionException(String msg, SQLException ex) {
		super(msg, ex);
	}

}
