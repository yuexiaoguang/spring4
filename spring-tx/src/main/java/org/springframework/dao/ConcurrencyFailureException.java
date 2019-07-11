package org.springframework.dao;

/**
 * 并发失败引发的异常.
 *
 * <p>应该对此异常进行子类化以指示失败的类型: 乐观锁定, 无法获取锁定等.
 */
@SuppressWarnings("serial")
public class ConcurrencyFailureException extends TransientDataAccessException {

	public ConcurrencyFailureException(String msg) {
		super(msg);
	}

	/**
	 * @param msg 详细信息
	 * @param cause 正在使用的数据访问API的根本原因
	 */
	public ConcurrencyFailureException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
