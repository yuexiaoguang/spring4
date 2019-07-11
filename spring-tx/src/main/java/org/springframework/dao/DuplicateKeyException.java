package org.springframework.dao;

/**
 * 尝试插入或更新数据导致违反主键或唯一约束时抛出的异常.
 * 请注意, 这不一定是纯粹的关系概念; 大多数数据库类型都需要唯一的主键.
 */
@SuppressWarnings("serial")
public class DuplicateKeyException extends DataIntegrityViolationException {

	public DuplicateKeyException(String msg) {
		super(msg);
	}

	/**
	 * @param msg 详细信息
	 * @param cause 正在使用的数据访问API的根本原因
	 */
	public DuplicateKeyException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
