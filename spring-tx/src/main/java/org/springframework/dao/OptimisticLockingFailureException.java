package org.springframework.dao;

/**
 * 乐观锁定违规抛出的异常.
 *
 * <p>O/R映射工具或自定义DAO实现将抛出此异常. 数据库本身通常<i>不</i>检测乐观锁定失败.
 */
@SuppressWarnings("serial")
public class OptimisticLockingFailureException extends ConcurrencyFailureException {

	public OptimisticLockingFailureException(String msg) {
		super(msg);
	}

	/**
	 * @param msg 详细信息
	 * @param cause 正在使用的数据访问API的根本原因
	 */
	public OptimisticLockingFailureException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
