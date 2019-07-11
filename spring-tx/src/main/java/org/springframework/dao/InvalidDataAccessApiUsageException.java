package org.springframework.dao;

/**
 * 不正确使用API​​时抛出的异常, 例如无法在执行前"编译"需要编译的查询对象.
 *
 * <p>这表示Java数据访问框架中的问题, 而不是底层数据访问基础结构.
 */
@SuppressWarnings("serial")
public class InvalidDataAccessApiUsageException extends NonTransientDataAccessException {

	public InvalidDataAccessApiUsageException(String msg) {
		super(msg);
	}

	/**
	 * @param msg 详细信息
	 * @param cause 正在使用的数据访问API的根本原因
	 */
	public InvalidDataAccessApiUsageException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
