package org.springframework.dao;

/**
 * 在更新期间未能获取锁定时抛出异常, 例如在"select for update"语句期间.
 */
@SuppressWarnings("serial")
public class CannotAcquireLockException extends PessimisticLockingFailureException {

	public CannotAcquireLockException(String msg) {
		super(msg);
	}

	/**
	 * @param msg 详细信息
	 * @param cause 正在使用的数据访问API的根本原因
	 */
	public CannotAcquireLockException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
