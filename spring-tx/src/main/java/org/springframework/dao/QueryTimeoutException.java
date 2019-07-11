package org.springframework.dao;

/**
 * 在查询超时时抛出异常.
 * 这可能有不同的原因, 具体取决于正在使用的数据库API, 但很可能在数据库完成查询之前中断或停止处理之后抛出.
 *
 * <p>用户代码捕获本机数据库异常或通过异常转换, 可能会抛出此异常.
 */
@SuppressWarnings("serial")
public class QueryTimeoutException extends TransientDataAccessException {

	public QueryTimeoutException(String msg) {
		super(msg);
	}

	/**
	 * @param msg 详细信息
	 * @param cause 正在使用的数据访问API的根本原因
	 */
	public QueryTimeoutException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
