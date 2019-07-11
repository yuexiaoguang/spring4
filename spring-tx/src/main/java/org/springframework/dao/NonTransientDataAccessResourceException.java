package org.springframework.dao;

/**
 * 当资源完全失败并且失败是永久性的时, 抛出数据访问异常.
 */
@SuppressWarnings("serial")
public class NonTransientDataAccessResourceException extends NonTransientDataAccessException {

	public NonTransientDataAccessResourceException(String msg) {
		super(msg);
	}

	/**
	 * @param msg 详细信息
	 * @param cause 正在使用的数据访问API的根本原因
	 */
	public NonTransientDataAccessResourceException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
