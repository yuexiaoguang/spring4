package org.springframework.dao;

/**
 * 当前进程是死锁失败者并且其事务回滚时抛出的一般异常.
 */
@SuppressWarnings("serial")
public class DeadlockLoserDataAccessException extends PessimisticLockingFailureException {

	/**
	 * @param msg 详细信息
	 * @param cause 正在使用的数据访问API的根本原因
	 */
	public DeadlockLoserDataAccessException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
