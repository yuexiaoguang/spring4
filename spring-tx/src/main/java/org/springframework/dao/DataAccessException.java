package org.springframework.dao;

import org.springframework.core.NestedRuntimeException;

/**
 * <a href="http://www.amazon.com/exec/obidos/tg/detail/-/0764543857/">Expert One-On-One J2EE Design and Development</a>
 * 中讨论的数据访问异常层次结构的根.
 * 有关此程序包动机的详细讨论, 请参阅本书的第9章.
 *
 * <p>此异常层次结构旨在让用户代码在不知道正在使用的特定数据访问API的详细信息的情况下查找和处理遇到的错误类型 (e.g. JDBC).
 * 因此, 可以在不知道正在使用JDBC的情况下对乐观锁失败做出反应.
 *
 * <p>由于此类是运行时异常, 因此如果任何错误被认为是致命的, 则不需要用户代码来捕获它或子类 (通常情况下).
 */
@SuppressWarnings("serial")
public abstract class DataAccessException extends NestedRuntimeException {

	public DataAccessException(String msg) {
		super(msg);
	}

	/**
	 * @param msg 详细信息
	 * @param cause 根本原因 (通常来自使用底层数据访问API, 如JDBC)
	 */
	public DataAccessException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
