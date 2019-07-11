package org.springframework.dao;

/**
 * 当错误地使用数据访问资源时抛出异常的根.
 * 例如, 在使用RDBMS时指定错误的SQL.
 * 特定于资源的子类由具体的数据访问包提供.
 */
@SuppressWarnings("serial")
public class InvalidDataAccessResourceUsageException extends NonTransientDataAccessException {

	public InvalidDataAccessResourceUsageException(String msg) {
		super(msg);
	}

	/**
	 * @param msg 详细信息
	 * @param cause 正在使用的数据访问API的根本原因
	 */
	public InvalidDataAccessResourceUsageException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
