package org.springframework.jca.cci;

import java.sql.SQLException;

import org.springframework.dao.InvalidDataAccessResourceUsageException;

/**
 * 以无效方式访问ResultSet时抛出异常.
 * 此类异常始终具有{@code java.sql.SQLException}根本原因.
 *
 * <p>当指定了无效的ResultSet列索引或名称时, 通常会发生这种情况.
 */
@SuppressWarnings("serial")
public class InvalidResultSetAccessException extends InvalidDataAccessResourceUsageException {

	public InvalidResultSetAccessException(String msg, SQLException ex) {
		super(ex.getMessage(), ex);
	}

}
