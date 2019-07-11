package org.springframework.dao;

/**
 * 当资源暂时失败并且操作可以重试时, 抛出的数据访问异常.
 */
@SuppressWarnings("serial")
public class TransientDataAccessResourceException extends TransientDataAccessException {

	public TransientDataAccessResourceException(String msg) {
		super(msg);
	}

	/**
	 * @param msg 详细信息
	 * @param cause 正在使用的数据访问API的根本原因
	 */
	public TransientDataAccessResourceException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
