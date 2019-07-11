package org.springframework.dao;

/**
 * Java类型和数据库类型不匹配时抛出异常: 例如, 尝试在RDBMS列中设置错误类型的对象.
 */
@SuppressWarnings("serial")
public class TypeMismatchDataAccessException extends InvalidDataAccessResourceUsageException {

	public TypeMismatchDataAccessException(String msg) {
		super(msg);
	}

	/**
	 * @param msg 详细信息
	 * @param cause 正在使用的数据访问API的根本原因
	 */
	public TypeMismatchDataAccessException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
