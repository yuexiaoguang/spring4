package org.springframework.dao;

/**
 * 如果无法检索某些预期数据, 则抛出异常, e.g. 通过已知标识符查找特定数据时.
 * O/R映射工具或DAO实现将抛出此异常.
 */
@SuppressWarnings("serial")
public class DataRetrievalFailureException extends NonTransientDataAccessException {

	public DataRetrievalFailureException(String msg) {
		super(msg);
	}

	/**
	 * @param msg 详细信息
	 * @param cause 正在使用的数据访问API的根本原因
	 */
	public DataRetrievalFailureException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
