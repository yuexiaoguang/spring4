package org.springframework.jdbc.datasource.lookup;

import org.springframework.dao.NonTransientDataAccessException;

/**
 * DataSourceLookup实现抛出的异常, 表示无法获取指定的DataSource.
 */
@SuppressWarnings("serial")
public class DataSourceLookupFailureException extends NonTransientDataAccessException {

	public DataSourceLookupFailureException(String msg) {
		super(msg);
	}

	/**
	 * @param msg 详细信息
	 * @param cause 根本原因 (通常来自使用底层查找API, 如JNDI)
	 */
	public DataSourceLookupFailureException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
