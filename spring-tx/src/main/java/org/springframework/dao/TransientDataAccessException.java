package org.springframework.dao;

/**
 * 被视为瞬态的数据访问异常层次结构的根 - 在重新执行操作时,
 * 如果先前失败的操作可能成功, 而没有应用程序级功能的任何干预.
 */
@SuppressWarnings("serial")
public abstract class TransientDataAccessException extends DataAccessException {

	public TransientDataAccessException(String msg) {
		super(msg);
	}

	/**
	 * @param msg 详细信息
	 * @param cause 根本原因 (通常来自使用底层数据访问API, 如JDBC)
	 */
	public TransientDataAccessException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
