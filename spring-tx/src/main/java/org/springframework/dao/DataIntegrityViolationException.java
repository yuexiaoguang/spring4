package org.springframework.dao;

/**
 * 尝试插入或更新数据导致违反完整性约束时抛出异常.
 * 请注意, 这不仅仅是一个关系概念; 大多数数据库类型都需要唯一的主键.
 */
@SuppressWarnings("serial")
public class DataIntegrityViolationException extends NonTransientDataAccessException {

	public DataIntegrityViolationException(String msg) {
		super(msg);
	}

	/**
	 * @param msg 详细信息
	 * @param cause 正在使用的数据访问API的根本原因
	 */
	public DataIntegrityViolationException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
