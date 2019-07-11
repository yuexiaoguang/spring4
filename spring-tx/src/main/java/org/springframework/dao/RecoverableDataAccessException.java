package org.springframework.dao;

/**
 * 如果应用程序执行某些恢复步骤并重试整个事务,
 * 或者在分布式事务, 事务分支的情况下, 先前失败的操作可能成功, 则抛出数据访问异常.
 * 恢复操作至少必须包括关闭当前连接并获取新连接.
 */
@SuppressWarnings("serial")
public class RecoverableDataAccessException extends DataAccessException {

	public RecoverableDataAccessException(String msg) {
		super(msg);
	}

	/**
	 * @param msg 详细信息
	 * @param cause 根本原因 (通常来自使用底层数据访问API, 如JDBC)
	 */
	public RecoverableDataAccessException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
