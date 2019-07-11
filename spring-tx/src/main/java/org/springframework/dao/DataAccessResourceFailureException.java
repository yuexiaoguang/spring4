package org.springframework.dao;

/**
 * 资源完全失败时抛出的数据访问异常: 例如, 如果无法使用JDBC连接到数据库.
 */
@SuppressWarnings("serial")
public class DataAccessResourceFailureException extends NonTransientDataAccessResourceException {

	public DataAccessResourceFailureException(String msg) {
		super(msg);
	}

	/**
	 * @param msg 详细信息
	 * @param cause 正在使用的数据访问API的根本原因
	 */
	public DataAccessResourceFailureException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
