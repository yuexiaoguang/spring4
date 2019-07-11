package org.springframework.dao;

/**
 * 由于更新冲突而无法在序列化模式下完成事务时抛出异常.
 */
@SuppressWarnings("serial")
public class CannotSerializeTransactionException extends PessimisticLockingFailureException {

	public CannotSerializeTransactionException(String msg) {
		super(msg);
	}

	/**
	 * @param msg 详细信息
	 * @param cause 正在使用的数据访问API的根本原因
	 */
	public CannotSerializeTransactionException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
