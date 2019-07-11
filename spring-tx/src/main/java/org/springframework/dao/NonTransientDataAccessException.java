package org.springframework.dao;

/**
 * 被视为非瞬态的数据访问异常层次结构的根 - 除非更正异常的原因, 否则相同操作的重试将失败.
 */
@SuppressWarnings("serial")
public abstract class NonTransientDataAccessException extends DataAccessException {

	public NonTransientDataAccessException(String msg) {
		super(msg);
	}

	/**
	 * @param msg 详细信息
	 * @param cause 根本原因 (通常来自使用底层数据访问API, 如JDBC)
	 */
	public NonTransientDataAccessException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
