package org.springframework.dao;

/**
 * 在悲观的锁定违规上抛出异常.
 * 如果遇到相应的数据库错误, 则由Spring的SQLException转换机制抛出.
 *
 * <p>用作更具体异常的超类, 例如CannotAcquireLockException 和 DeadlockLoserDataAccessException.
 */
@SuppressWarnings("serial")
public class PessimisticLockingFailureException extends ConcurrencyFailureException {

	public PessimisticLockingFailureException(String msg) {
		super(msg);
	}

	/**
	 * @param msg 详细信息
	 * @param cause 正在使用的数据访问API的根本原因
	 */
	public PessimisticLockingFailureException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
