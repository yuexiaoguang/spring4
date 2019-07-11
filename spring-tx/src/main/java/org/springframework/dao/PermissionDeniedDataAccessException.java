package org.springframework.dao;

/**
 * 当底层资源拒绝访问特定元素时抛出异常, 例如特定数据库表.
 */
@SuppressWarnings("serial")
public class PermissionDeniedDataAccessException extends NonTransientDataAccessException {

	/**
	 * @param msg 详细信息
	 * @param cause 来自底层数据访问API的根本原因, 例如JDBC
	 */
	public PermissionDeniedDataAccessException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
