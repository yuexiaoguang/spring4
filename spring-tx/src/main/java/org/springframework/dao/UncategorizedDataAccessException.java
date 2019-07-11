package org.springframework.dao;

/**
 * 正常的超类, 当无法区分比“底层资源出错”更具体的东西时: 例如, 来自JDBC的SQLException无法更准确地指出.
 */
@SuppressWarnings("serial")
public abstract class UncategorizedDataAccessException extends NonTransientDataAccessException {

	/**
	 * @param msg 详细信息
	 * @param cause 底层数据访问API抛出的异常
	 */
	public UncategorizedDataAccessException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
